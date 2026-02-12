package org.pentaho.di.trans.steps.textsafety;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * Runtime data container for the Text Safety Check step.
 * Holds state that is needed during row processing.
 */
public class TextSafetyCheckStepData extends BaseStepData implements StepDataInterface {

    /** Output row metadata structure (input fields + appended result fields). */
    public RowMetaInterface outputRowMeta;

    /** Index of the input text field within the incoming row. */
    public int inputTextFieldIndex = -1;

    public TextSafetyCheckStepData() {
        super();
    }
}

