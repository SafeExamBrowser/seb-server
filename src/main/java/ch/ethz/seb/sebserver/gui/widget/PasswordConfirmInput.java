package ch.ethz.seb.sebserver.gui.widget;

import ch.ethz.seb.sebserver.gbl.util.Cryptor;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

public class PasswordConfirmInput extends Composite {

    private static final LocTextKey VAL_CONFIRM_PWD_TEXT_KEY =
            new LocTextKey("sebserver.examconfig.props.validation.password.confirm");

    final WidgetFactory widgetFactory;
    final Cryptor cryptor;
    private final PasswordInput password;
    private final PasswordInput confirm;
    private final Label errorLabel;

    public PasswordConfirmInput(
            final Composite parent,
            final WidgetFactory widgetFactory,
            final Cryptor cryptor,
            final LocTextKey ariaLabel,
            final LocTextKey testLabel) {

        super(parent, SWT.NONE);

        this.widgetFactory = widgetFactory;
        this.cryptor = cryptor;
        final GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 10;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 10;
        this.setLayout(gridLayout);
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        this.password = new PasswordInput(parent, widgetFactory, ariaLabel, testLabel);
        this.confirm = new PasswordInput(parent, widgetFactory, ariaLabel, testLabel);
        this.errorLabel = new Label(parent, SWT.NONE);
        final GridData gridData = new GridData(SWT.FILL, SWT.TOP, true, true);
        errorLabel.setLayoutData(gridData);
        errorLabel.setVisible(false);
        errorLabel.setData(RWT.CUSTOM_VARIANT, WidgetFactory.CustomVariant.ERROR.key);

        final Listener valueChangeEventListener = event -> checkError();
        this.password.addListener(SWT.FocusOut, valueChangeEventListener);
        this.password.addListener(SWT.Traverse, valueChangeEventListener);
        this.confirm.addListener(SWT.FocusOut, valueChangeEventListener);
        this.confirm.addListener(SWT.Traverse, valueChangeEventListener);
    }

    public void setValue(final CharSequence value) {
        if (StringUtils.isBlank(value)) {
            this.password.setValue(null);
            this.confirm.setValue(null);
        } else {
            final CharSequence val = cryptor.decrypt(value).getOr(value);
            this.password.setValue(val);
            this.confirm.setValue(val);
        }
    }

    public CharSequence getValue() {
        if (!checkError()) {
            return null;
        }
        final CharSequence value = password.getValue();
        if (StringUtils.isNotBlank(value)) {
            return cryptor.encrypt(value).getOr(value);
        } else {
            return null;
        }
     }

    public boolean hasError() {
        return checkError();
    }

    public void clearError() {
        this.errorLabel.setVisible(false);
        this.errorLabel.setText(StringUtils.EMPTY);
    }

    private boolean checkError() {
        clearError();

        final CharSequence pwd = this.password.getValue();
        final CharSequence confirm = this.confirm.getValue();

        if (pwd == null) {
            return false;
        }

        if (pwd.length() > 255) {
            final LocTextKey errmsg = new LocTextKey("sebserver.form.validation.fieldError.size.max", 256);
            errorLabel.setText(widgetFactory.getI18nSupport().getText(errmsg));
            errorLabel.setVisible(true);
            return true;
        }

        if (!pwd.equals(confirm)) {
            errorLabel.setText(widgetFactory.getI18nSupport().getText(VAL_CONFIRM_PWD_TEXT_KEY));
            errorLabel.setVisible(true);
            return true;
        }

        return false;
    }
}
