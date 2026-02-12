package org.pentaho.di.ui.trans.steps.textsafety;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.steps.textsafety.TextSafetyCheckStepMeta;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

/**
 * SWT dialog for the Text Safety Check step configuration.
 */
public class TextSafetyCheckStepDialog extends BaseStepDialog implements StepDialogInterface {

    private TextSafetyCheckStepMeta meta;

    private Combo wInputField;
    private Text wServiceUrl;
    private Text wThreshold;
    private Text wOutputSafe;
    private Text wOutputCategory;
    private Text wOutputScore;

    public TextSafetyCheckStepDialog(Shell parent, Object baseStepMeta, TransMeta transMeta, String stepname) {
        super(parent, (BaseStepMeta) baseStepMeta, transMeta, stepname);
        meta = (TextSafetyCheckStepMeta) baseStepMeta;
    }

    @Override
    public String open() {
        Shell parent = getParent();
        Display display = parent.getDisplay();
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN);
        props.setLook(shell);
        setShellImage(shell, meta);

        ModifyListener lsMod = e -> meta.setChanged();
        changed = meta.hasChanged();

        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = Const.FORM_MARGIN;
        formLayout.marginHeight = Const.FORM_MARGIN;
        shell.setLayout(formLayout);
        shell.setText("Text Safety Check");

        int middle = props.getMiddlePct();
        int margin = Const.MARGIN;

        // Step name
        wlStepname = new Label(shell, SWT.RIGHT);
        wlStepname.setText("Step Name");
        props.setLook(wlStepname);
        fdlStepname = new FormData();
        fdlStepname.left = new FormAttachment(0, 0);
        fdlStepname.right = new FormAttachment(middle, -margin);
        fdlStepname.top = new FormAttachment(0, margin);
        wlStepname.setLayoutData(fdlStepname);

        wStepname = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        wStepname.setText(stepname);
        props.setLook(wStepname);
        wStepname.addModifyListener(lsMod);
        fdStepname = new FormData();
        fdStepname.left = new FormAttachment(middle, 0);
        fdStepname.top = new FormAttachment(0, margin);
        fdStepname.right = new FormAttachment(100, 0);
        wStepname.setLayoutData(fdStepname);

        // Input text field (dropdown)
        Label wlInput = new Label(shell, SWT.RIGHT);
        wlInput.setText("Input Text Field");
        props.setLook(wlInput);
        FormData fdlInput = new FormData();
        fdlInput.left = new FormAttachment(0, 0);
        fdlInput.right = new FormAttachment(middle, -margin);
        fdlInput.top = new FormAttachment(wStepname, margin);
        wlInput.setLayoutData(fdlInput);

        wInputField = new Combo(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wInputField);
        wInputField.addModifyListener(lsMod);
        FormData fdInput = new FormData();
        fdInput.left = new FormAttachment(middle, 0);
        fdInput.top = new FormAttachment(wStepname, margin);
        fdInput.right = new FormAttachment(100, 0);
        wInputField.setLayoutData(fdInput);

        // Populate upstream field names
        try {
            RowMetaInterface prevFields = transMeta.getPrevStepFields(stepname);
            if (prevFields != null) {
                for (String fn : prevFields.getFieldNames()) {
                    wInputField.add(fn);
                }
            }
        } catch (KettleException e) {
            logDebug("Unable to get previous step fields", e);
        }

        // Service URL
        Label wlUrl = new Label(shell, SWT.RIGHT);
        wlUrl.setText("Service URL");
        props.setLook(wlUrl);
        FormData fdlUrl = new FormData();
        fdlUrl.left = new FormAttachment(0, 0);
        fdlUrl.right = new FormAttachment(middle, -margin);
        fdlUrl.top = new FormAttachment(wInputField, margin);
        wlUrl.setLayoutData(fdlUrl);

        wServiceUrl = new Text(shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(wServiceUrl);
        wServiceUrl.addModifyListener(lsMod);
        FormData fdUrl = new FormData();
        fdUrl.left = new FormAttachment(middle, 0);
        fdUrl.top = new FormAttachment(wInputField, margin);
        fdUrl.right = new FormAttachment(100, 0);
        wServiceUrl.setLayoutData(fdUrl);

        // Threshold & output field names
        wThreshold = addLabeledText(shell, "Risk Threshold (0-1)", wServiceUrl, middle, margin, lsMod);
        wOutputSafe = addLabeledText(shell, "Output: Is Safe", wThreshold, middle, margin, lsMod);
        wOutputCategory = addLabeledText(shell, "Output: Risk Category", wOutputSafe, middle, margin, lsMod);
        wOutputScore = addLabeledText(shell, "Output: Risk Score", wOutputCategory, middle, margin, lsMod);

        // OK / Cancel buttons
        wOK = new Button(shell, SWT.PUSH);
        wOK.setText("OK");
        wCancel = new Button(shell, SWT.PUSH);
        wCancel.setText("Cancel");
        setButtonPositions(new Button[]{wOK, wCancel}, margin, wOutputScore);

        wOK.addListener(SWT.Selection, e -> ok());
        wCancel.addListener(SWT.Selection, e -> cancel());

        // Handle window close via 'X' button
        shell.addListener(SWT.Close, e -> cancel());

        // Load saved values into UI
        getData();

        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return stepname;
    }

    /**
     * Helper: creates a Label + Text pair with FormLayout positioning.
     */
    private Text addLabeledText(Shell parent, String labelText, Control above,
                                int middle, int margin, ModifyListener lsMod) {
        Label lbl = new Label(parent, SWT.RIGHT);
        lbl.setText(labelText);
        props.setLook(lbl);
        FormData fdl = new FormData();
        fdl.left = new FormAttachment(0, 0);
        fdl.right = new FormAttachment(middle, -margin);
        fdl.top = new FormAttachment(above, margin);
        lbl.setLayoutData(fdl);

        Text txt = new Text(parent, SWT.SINGLE | SWT.LEFT | SWT.BORDER);
        props.setLook(txt);
        txt.addModifyListener(lsMod);
        FormData fdt = new FormData();
        fdt.left = new FormAttachment(middle, 0);
        fdt.top = new FormAttachment(above, margin);
        fdt.right = new FormAttachment(100, 0);
        txt.setLayoutData(fdt);
        return txt;
    }

    /** Loads saved configuration from Meta into the UI controls. */
    private void getData() {
        wStepname.selectAll();
        if (meta.getInputTextField() != null) {
            wInputField.setText(meta.getInputTextField());
        }
        if (meta.getServiceUrl() != null) {
            wServiceUrl.setText(meta.getServiceUrl());
        }
        wThreshold.setText(String.valueOf(meta.getThreshold()));
        if (meta.getOutputSafeField() != null) {
            wOutputSafe.setText(meta.getOutputSafeField());
        }
        if (meta.getOutputCategoryField() != null) {
            wOutputCategory.setText(meta.getOutputCategoryField());
        }
        if (meta.getOutputScoreField() != null) {
            wOutputScore.setText(meta.getOutputScoreField());
        }
    }

    /** OK button — writes UI values back to Meta. */
    private void ok() {
        if (Const.isEmpty(wStepname.getText())) {
            return;
        }
        stepname = wStepname.getText();
        meta.setInputTextField(wInputField.getText());
        meta.setServiceUrl(wServiceUrl.getText());
        try {
            meta.setThreshold(Double.parseDouble(wThreshold.getText()));
        } catch (NumberFormatException e) {
            meta.setThreshold(0.5);
        }
        meta.setOutputSafeField(wOutputSafe.getText());
        meta.setOutputCategoryField(wOutputCategory.getText());
        meta.setOutputScoreField(wOutputScore.getText());
        meta.setChanged();
        dispose();
    }

    /** Cancel button — reverts changes and closes. */
    private void cancel() {
        stepname = null;
        meta.setChanged(changed);
        dispose();
    }
}
