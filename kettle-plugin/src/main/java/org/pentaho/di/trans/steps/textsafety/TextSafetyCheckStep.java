package org.pentaho.di.trans.steps.textsafety;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Core processing logic for the Text Safety Check step.
 * <p>
 * Reads each row, sends the text content to the Python safety detection service
 * via HTTP POST, and appends the detection result fields (is_safe, risk_category,
 * risk_score) to the output row.
 * </p>
 */
public class TextSafetyCheckStep extends BaseStep implements StepInterface {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private TextSafetyCheckStepMeta meta;
    private TextSafetyCheckStepData data;

    public TextSafetyCheckStep(StepMeta stepMeta, StepDataInterface stepDataInterface,
                               int copyNr, TransMeta transMeta, Trans trans) {
        super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
        meta = (TextSafetyCheckStepMeta) smi;
        data = (TextSafetyCheckStepData) sdi;
        return super.init(smi, sdi);
    }

    @Override
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
        meta = (TextSafetyCheckStepMeta) smi;
        data = (TextSafetyCheckStepData) sdi;

        Object[] row = getRow();
        if (row == null) {
            setOutputDone();
            return false;
        }

        // First row: resolve field index and build output row metadata
        if (first) {
            first = false;
            data.outputRowMeta = getInputRowMeta().clone();
            meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);
            data.inputTextFieldIndex = getInputRowMeta().indexOfValue(meta.getInputTextField());
            if (data.inputTextFieldIndex < 0) {
                throw new KettleException("Input text field not found: " + meta.getInputTextField());
            }
        }

        // Extract the text value from the current row
        String textValue = getInputRowMeta().getString(row, data.inputTextFieldIndex);

        boolean isSafe = true;
        String riskCategory = "sec";
        double riskScore = 0.0;

        if (Const.isEmpty(textValue)) {
            // Treat empty / null text as safe, skip API call
            logBasic("Skipping empty text value at row " + getLinesRead());
        } else {
            try {
                String response = callSafetyApi(textValue);
                logDebug("API response: " + response);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();

                isSafe = json.get("is_safe").getAsBoolean();
                riskScore = json.get("risk_score").getAsDouble();

                if (json.has("risk_category") && !json.get("risk_category").isJsonNull()) {
                    riskCategory = json.get("risk_category").getAsString();
                } else {
                    riskCategory = "sec";
                }
            } catch (Exception e) {
                logError("Safety API call failed: " + e.getMessage(), e);
                isSafe = false;
                riskCategory = "api_error";
                riskScore = 1.0;
            }
        }

        // Append the 3 output fields to the row
        Object[] outputRow = RowDataUtil.addValueData(row, getInputRowMeta().size(), isSafe);
        outputRow = RowDataUtil.addValueData(outputRow, getInputRowMeta().size() + 1, riskCategory);
        outputRow = RowDataUtil.addValueData(outputRow, getInputRowMeta().size() + 2, riskScore);

        putRow(data.outputRowMeta, outputRow);
        return true;
    }

    /**
     * Calls the Python text_filter_service via HTTP POST.
     *
     * @param text the text content to check
     * @return the raw JSON response body
     * @throws IOException if the HTTP call fails
     */
    private String callSafetyApi(String text) throws IOException {
        String serviceUrl = environmentSubstitute(meta.getServiceUrl());
        URL url = new URL(serviceUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);

            JsonObject reqBody = new JsonObject();
            reqBody.addProperty("text", text);
            reqBody.addProperty("threshold", meta.getThreshold());
            byte[] payload = reqBody.toString().getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int status = conn.getResponseCode();
            if (status != 200) {
                String errorBody = readStream(conn.getErrorStream());
                throw new IOException("API returned HTTP " + status + ": " + errorBody);
            }

            return readStream(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Reads an InputStream fully into a String.
     */
    private String readStream(java.io.InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}

