/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.eclipse.rap.fileupload.FileDetails;
import org.eclipse.rap.fileupload.FileUploadHandler;
import org.eclipse.rap.fileupload.FileUploadReceiver;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.Constants;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.AriaRole;

public class FileUploadSelection extends Composite {

    private static final Logger log = LoggerFactory.getLogger(FileUploadSelection.class);

    private static final long serialVersionUID = 5800153475027387363L;

    private static final LocTextKey PLEASE_SELECT_TEXT =
            new LocTextKey("sebserver.overall.upload");

    private final I18nSupport i18nSupport;
    private final List<String> supportedFileExtensions = new ArrayList<>();

    private final boolean readonly;
    private final FileUpload fileUpload;
    private final Label fileName;

    private Consumer<String> errorHandler;
    private InputStream inputStream;
    private final FileUploadHandler uploadHandler;
    private final InputReceiver inputReceiver;
    private boolean selection = false;

    public FileUploadSelection(
            final Composite parent,
            final I18nSupport i18nSupport,
            final Collection<String> supportedFiles,
            final boolean readonly,
            final String testKey,
            final LocTextKey ariaLabel) {

        super(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.horizontalSpacing = 5;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        super.setLayout(gridLayout);

        this.i18nSupport = i18nSupport;
        this.readonly = readonly;

        if (readonly) {
            this.fileName = new Label(this, SWT.NONE);
            this.fileName.setText(i18nSupport.getText(PLEASE_SELECT_TEXT));
            this.fileName.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
            if (ariaLabel != null) {
                WidgetFactory.setARIALabel(this.fileName, i18nSupport.getText(ariaLabel));
            }
            if (testKey != null) {
                WidgetFactory.setTestId(this.fileName, testKey);
            }
            this.fileUpload = null;
            this.uploadHandler = null;
            this.inputReceiver = null;
        } else {
            this.fileUpload = new FileUpload(this, SWT.NONE);
            this.fileUpload.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, true));
            this.fileUpload.setImage(WidgetFactory.ImageIcon.IMPORT.getImage(parent.getDisplay()));

            if (ariaLabel != null) {
                WidgetFactory.setARIALabel(this.fileUpload, i18nSupport.getText(ariaLabel));
            }
            if (testKey != null) {
                WidgetFactory.setTestId(this.fileUpload, testKey);
            }
            WidgetFactory.setARIARole(this.fileUpload, AriaRole.button);

            this.fileUpload.setToolTipText(Utils.formatLineBreaks(this.i18nSupport.getText(PLEASE_SELECT_TEXT)));
            this.inputReceiver = new InputReceiver();
            this.uploadHandler = new FileUploadHandler(this.inputReceiver);

            if (supportedFiles != null && !supportedFiles.isEmpty()) {
                this.fileUpload.setFilterExtensions(supportedFiles.toArray(new String[supportedFiles.size()]));
            }

            this.fileName = new Label(this, SWT.NONE);
            this.fileName.setText(i18nSupport.getText(PLEASE_SELECT_TEXT));
            this.fileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));

            this.fileUpload.addListener(SWT.Selection, this::selectFile);
            this.fileName.addListener(SWT.Selection, this::selectFile);
        }
    }

    private void selectFile(final Event event) {
        this.selection = true;
        final String fileName = FileUploadSelection.this.fileUpload.getFileName();
        if (fileName == null || !fileSupported(fileName)) {
            if (FileUploadSelection.this.errorHandler != null) {
                final String text = this.i18nSupport.getText(new LocTextKey(
                        "sebserver.overall.upload.unsupported.file",
                        this.supportedFileExtensions.toString()),
                        "Unsupported image file type selected");
                FileUploadSelection.this.errorHandler.accept(text);
            }
            return;
        }
        FileUploadSelection.this.fileUpload.submit(this.uploadHandler.getUploadUrl());
        FileUploadSelection.this.fileName.setText(fileName);
        FileUploadSelection.this.errorHandler.accept(null);
    }

    public void close() {
        if (this.inputReceiver != null) {
            this.inputReceiver.close();
        }
    }

    @Override
    public void dispose() {
        if (this.uploadHandler != null) {
            this.uploadHandler.dispose();
        }
        super.dispose();
    }

    public String getFileName() {
        if (this.fileName != null) {
            return this.fileName.getText();
        }

        return Constants.EMPTY_NOTE;
    }

    public void setFileName(final String fileName) {
        if (this.fileName != null && fileName != null) {
            this.fileName.setText(fileName);
        }
    }

    public boolean hasFileSelection() {
        return this.selection;
    }

    public void setSelection(final boolean selection) {
        this.selection = selection;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    @Override
    public void update() {
        if (this.inputStream != null) {
            this.fileName.setText(this.i18nSupport.getText(PLEASE_SELECT_TEXT));
        }
        if (!this.readonly) {
            this.fileUpload.setToolTipText(Utils.formatLineBreaks(this.i18nSupport.getText(PLEASE_SELECT_TEXT)));
        }
    }

    public FileUploadSelection setErrorHandler(final Consumer<String> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public FileUploadSelection withSupportFor(final String fileExtension) {
        this.supportedFileExtensions.add(fileExtension);
        return this;
    }

    private boolean fileSupported(final String fileName) {
        return this.supportedFileExtensions
                .stream()
                .anyMatch(fileType -> fileName.toUpperCase(Locale.ROOT)
                        .endsWith(fileType.toUpperCase(Locale.ROOT)));
    }

    private final class InputReceiver extends FileUploadReceiver {
        private PipedInputStream pIn = null;
        private PipedOutputStream pOut = null;

        @Override
        public void receive(final InputStream stream, final FileDetails details) throws IOException {
            if (this.pIn != null || this.pOut != null) {
                throw new IllegalStateException("InputReceiver already in use");
            }

            this.pIn = new PipedInputStream();
            this.pOut = new PipedOutputStream(this.pIn);

            FileUploadSelection.this.inputStream = this.pIn;

            try {
                IOUtils.copyLarge(stream, this.pOut);
            } catch (final Exception e) {
                log.warn("IO error: {}", e.getMessage());
            } finally {
                close();
            }
        }

        void close() {
            IOUtils.closeQuietly(this.pOut);
        }
    }

}
