/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.widget;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.fileupload.FileDetails;
import org.eclipse.rap.fileupload.FileUploadHandler;
import org.eclipse.rap.fileupload.FileUploadReceiver;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gbl.util.Utils;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;
import ch.ethz.seb.sebserver.gui.service.i18n.LocTextKey;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;
import ch.ethz.seb.sebserver.gui.widget.WidgetFactory.AriaRole;

public final class ImageUploadSelection extends Composite {

    private static final long serialVersionUID = 368264811155804533L;
    private static final Logger log = LoggerFactory.getLogger(ImageUploadSelection.class);

    public static final Set<String> SUPPORTED_IMAGE_FILES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ".png",
            ".jpg",
            ".jpeg")));

    private final ServerPushService serverPushService;

    private final Composite imageCanvas;
    private final FileUpload fileUpload;
    private final int maxWidth;
    private final int maxHeight;

    private Consumer<String> errorHandler;
    private String imageBase64 = null;
    private boolean loadNewImage = false;
    private boolean imageLoaded = false;

    ImageUploadSelection(
            final Composite parent,
            final ServerPushService serverPushService,
            final I18nSupport i18nSupport,
            final boolean readonly,
            final int maxWidth,
            final int maxHeight,
            final LocTextKey ariaLabel) {

        super(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.horizontalSpacing = 0;
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.marginLeft = 0;
        gridLayout.verticalSpacing = 0;
        super.setLayout(gridLayout);

        this.serverPushService = serverPushService;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;

        if (!readonly) {
            this.fileUpload = new FileUpload(this, SWT.NONE);
            this.fileUpload.setImage(WidgetFactory.ImageIcon.IMPORT.getImage(parent.getDisplay()));
            final GridData gridData = new GridData(SWT.LEFT, SWT.TOP, false, false);
            gridData.horizontalIndent = 0;
            this.fileUpload.setLayoutData(gridData);

            if (ariaLabel != null) {
                WidgetFactory.setARIALabel(this.fileUpload, i18nSupport.getText(ariaLabel));
            }
            WidgetFactory.setARIARole(this.fileUpload, AriaRole.button);

            final FileUploadHandler uploadHandler = new FileUploadHandler(new ImageReceiver());
            this.fileUpload.addListener(SWT.Selection, event -> {
                final String fileName = ImageUploadSelection.this.fileUpload.getFileName();
                if (fileName == null || !fileSupported(fileName)) {
                    if (ImageUploadSelection.this.errorHandler != null) {
                        final String text = i18nSupport.getText(
                                "sebserver.institution.form.logoImage.unsupportedFileType",
                                "Unsupported image file type selected");
                        ImageUploadSelection.this.errorHandler.accept(text);
                    }

                    log.warn("Unsupported image file selected: {}", fileName);

                    return;
                }
                ImageUploadSelection.this.loadNewImage = true;
                ImageUploadSelection.this.imageLoaded = false;
                ImageUploadSelection.this.fileUpload.submit(uploadHandler.getUploadUrl());

                ImageUploadSelection.this.serverPushService.runServerPush(
                        new ServerPushContext(
                                ImageUploadSelection.this,
                                ImageUploadSelection::uploadInProgress,
                                error -> {
                                    log.error("Failed to upload image: {}", error.getMessage());
                                    return false;
                                }),
                        200,
                        ImageUploadSelection::update);
            });
        } else {
            this.fileUpload = null;
        }

        this.imageCanvas = new Composite(this, SWT.NONE);
        final GridData canvas = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.imageCanvas.setLayoutData(canvas);
    }

    public void setErrorHandler(final Consumer<String> errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setSelectionText(final String text) {
        if (this.fileUpload != null) {
            this.fileUpload.setToolTipText(Utils.formatLineBreaks(text));
        }
    }

    public String getImageBase64() {
        return this.imageBase64;
    }

    public void setImageBase64(final String imageBase64) {
        if (StringUtils.isBlank(imageBase64)) {
            return;
        }

        this.imageBase64 = imageBase64;
        final Base64InputStream input = new Base64InputStream(
                new ByteArrayInputStream(imageBase64.getBytes(StandardCharsets.UTF_8)), false);

        setImage(this, input);
    }

    private static boolean uploadInProgress(final ServerPushContext context) {
        final ImageUploadSelection imageUpload = (ImageUploadSelection) context.getAnchor();
        return imageUpload.loadNewImage && !imageUpload.imageLoaded;
    }

    private static void update(final ServerPushContext context) {
        final ImageUploadSelection imageUpload = (ImageUploadSelection) context.getAnchor();
        if (imageUpload.imageBase64 != null
                && imageUpload.loadNewImage
                && imageUpload.imageLoaded) {

            final Base64InputStream input = new Base64InputStream(
                    new ByteArrayInputStream(
                            imageUpload.imageBase64.getBytes(StandardCharsets.UTF_8)),
                    false);

            setImage(imageUpload, input);
            context.layout();
            imageUpload.layout();
            imageUpload.loadNewImage = false;
            imageUpload.errorHandler.accept(null);
        }
    }

    private static void setImage(final ImageUploadSelection imageUpload, final Base64InputStream input) {
        imageUpload.imageCanvas.setData(RWT.CUSTOM_VARIANT, "bgLogoNoImage");

        final Image image = new Image(imageUpload.imageCanvas.getDisplay(), input);
        final Rectangle imageBounds = image.getBounds();
        final int width = Math.min(imageBounds.width, imageUpload.maxWidth);
        final int height = Math.min(imageBounds.height, imageUpload.maxHeight);
        final ImageData imageData = image.getImageData().scaledTo(width, height);
        imageUpload.imageCanvas.setBackgroundImage(new Image(imageUpload.imageCanvas.getDisplay(), imageData));
    }

    private static boolean fileSupported(final String fileName) {
        return SUPPORTED_IMAGE_FILES
                .stream()
                .anyMatch(fileType -> fileName.toUpperCase(Locale.ROOT)
                        .endsWith(fileType.toUpperCase(Locale.ROOT)));
    }

    private final class ImageReceiver extends FileUploadReceiver {
        @Override
        public void receive(final InputStream stream, final FileDetails details) throws IOException {

            try {
                final String contentType = details.getContentType();
                if (contentType != null && contentType.startsWith("image")) {
                    ImageUploadSelection.this.imageBase64 = Base64.getEncoder()
                            .encodeToString(IOUtils.toByteArray(stream));
                }
            } catch (final Exception e) {
                log.error("Error while trying to upload image", e);
            } finally {
                ImageUploadSelection.this.imageLoaded = true;
                stream.close();
            }
        }
    }

}
