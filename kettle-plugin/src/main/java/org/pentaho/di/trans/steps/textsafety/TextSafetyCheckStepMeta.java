package org.pentaho.di.trans.steps.textsafety;

import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBoolean;
import org.pentaho.di.core.row.value.ValueMetaNumber;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * Metadata (configuration) class for the Text Safety Check step.
 * <p>
 * Stores user-configurable properties such as the input text field name,
 * the safety service URL, risk threshold, and output field names.
 * </p>
 */
@Step(
    id = "TextSafetyCheck",
    name = "Text Safety Check",
    description = "Check text content safety risks using a local model",
    image = "text_safety.svg",
    categoryDescription = "Transform"
)
public class TextSafetyCheckStepMeta extends BaseStepMeta implements StepMetaInterface {

    private static final String DEFAULT_SERVICE_URL = "http://localhost:8001/api/check";
    private static final double DEFAULT_THRESHOLD = 0.5;

    // User-configurable fields
    private String inputTextField = "";
    private String serviceUrl = DEFAULT_SERVICE_URL;
    private double threshold = DEFAULT_THRESHOLD;
    private String outputSafeField = "is_safe";
    private String outputCategoryField = "risk_category";
    private String outputScoreField = "risk_score";

    public TextSafetyCheckStepMeta() {
        super();
    }

    // --- Getters / Setters ---

    public String getInputTextField() { return inputTextField; }
    public void setInputTextField(String v) { this.inputTextField = v; }

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String v) { this.serviceUrl = v; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double v) { this.threshold = v; }

    public String getOutputSafeField() { return outputSafeField; }
    public void setOutputSafeField(String v) { this.outputSafeField = v; }

    public String getOutputCategoryField() { return outputCategoryField; }
    public void setOutputCategoryField(String v) { this.outputCategoryField = v; }

    public String getOutputScoreField() { return outputScoreField; }
    public void setOutputScoreField(String v) { this.outputScoreField = v; }

    @Override
    public Object clone() {
        TextSafetyCheckStepMeta copy = (TextSafetyCheckStepMeta) super.clone();
        return copy;
    }

    @Override
    public void setDefault() {
        inputTextField = "";
        serviceUrl = DEFAULT_SERVICE_URL;
        threshold = DEFAULT_THRESHOLD;
        outputSafeField = "is_safe";
        outputCategoryField = "risk_category";
        outputScoreField = "risk_score";
    }

    @Override
    public String getXML() {
        StringBuilder xml = new StringBuilder();
        xml.append("    ").append(XMLHandler.addTagValue("input_text_field", inputTextField));
        xml.append("    ").append(XMLHandler.addTagValue("service_url", serviceUrl));
        xml.append("    ").append(XMLHandler.addTagValue("threshold", threshold));
        xml.append("    ").append(XMLHandler.addTagValue("output_safe_field", outputSafeField));
        xml.append("    ").append(XMLHandler.addTagValue("output_category_field", outputCategoryField));
        xml.append("    ").append(XMLHandler.addTagValue("output_score_field", outputScoreField));
        return xml.toString();
    }

    @Override
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
        inputTextField = Const.NVL(XMLHandler.getTagValue(stepnode, "input_text_field"), "");
        serviceUrl = Const.NVL(XMLHandler.getTagValue(stepnode, "service_url"), DEFAULT_SERVICE_URL);
        String th = XMLHandler.getTagValue(stepnode, "threshold");
        threshold = (th != null) ? Double.parseDouble(th) : DEFAULT_THRESHOLD;
        outputSafeField = Const.NVL(XMLHandler.getTagValue(stepnode, "output_safe_field"), "is_safe");
        outputCategoryField = Const.NVL(XMLHandler.getTagValue(stepnode, "output_category_field"), "risk_category");
        outputScoreField = Const.NVL(XMLHandler.getTagValue(stepnode, "output_score_field"), "risk_score");
    }

    @Override
    public void saveRep(Repository rep, IMetaStore metaStore, ObjectId idTransformation, ObjectId idStep)
            throws KettleException {
        rep.saveStepAttribute(idTransformation, idStep, "input_text_field", inputTextField);
        rep.saveStepAttribute(idTransformation, idStep, "service_url", serviceUrl);
        rep.saveStepAttribute(idTransformation, idStep, "threshold", threshold);
        rep.saveStepAttribute(idTransformation, idStep, "output_safe_field", outputSafeField);
        rep.saveStepAttribute(idTransformation, idStep, "output_category_field", outputCategoryField);
        rep.saveStepAttribute(idTransformation, idStep, "output_score_field", outputScoreField);
    }

    @Override
    public void readRep(Repository rep, IMetaStore metaStore, ObjectId idStep, List<DatabaseMeta> databases)
            throws KettleException {
        inputTextField = Const.NVL(rep.getStepAttributeString(idStep, "input_text_field"), "");
        serviceUrl = Const.NVL(rep.getStepAttributeString(idStep, "service_url"), DEFAULT_SERVICE_URL);
        String th = rep.getStepAttributeString(idStep, "threshold");
        threshold = (th != null && !th.isEmpty()) ? Double.parseDouble(th) : DEFAULT_THRESHOLD;
        outputSafeField = Const.NVL(rep.getStepAttributeString(idStep, "output_safe_field"), "is_safe");
        outputCategoryField = Const.NVL(rep.getStepAttributeString(idStep, "output_category_field"), "risk_category");
        outputScoreField = Const.NVL(rep.getStepAttributeString(idStep, "output_score_field"), "risk_score");
    }


    @Override
    public void getFields(RowMetaInterface inputRowMeta, String stepName, RowMetaInterface[] info,
                          StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore)
            throws KettleStepException {
        // Append output fields to the row metadata
        ValueMetaInterface safeMeta = new ValueMetaBoolean(outputSafeField);
        safeMeta.setOrigin(stepName);
        inputRowMeta.addValueMeta(safeMeta);

        ValueMetaInterface catMeta = new ValueMetaString(outputCategoryField);
        catMeta.setOrigin(stepName);
        inputRowMeta.addValueMeta(catMeta);

        ValueMetaInterface scoreMeta = new ValueMetaNumber(outputScoreField);
        scoreMeta.setOrigin(stepName);
        inputRowMeta.addValueMeta(scoreMeta);
    }

    @Override
    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface,
                                  int copyNr, TransMeta transMeta, Trans trans) {
        return new TextSafetyCheckStep(stepMeta, stepDataInterface, copyNr, transMeta, trans);
    }

    @Override
    public StepDataInterface getStepData() {
        return new TextSafetyCheckStepData();
    }

    @Override
    public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
                      RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info,
                      VariableSpace space, Repository repository, IMetaStore metaStore) {
        if (Const.isEmpty(inputTextField)) {
            remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR,
                "Input text field must be specified", stepMeta));
        }
        if (Const.isEmpty(serviceUrl)) {
            remarks.add(new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR,
                "Safety service URL must be specified", stepMeta));
        }
    }
}
