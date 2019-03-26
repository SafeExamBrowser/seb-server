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
import java.util.Base64;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rap.fileupload.FileDetails;
import org.eclipse.rap.fileupload.FileUploadHandler;
import org.eclipse.rap.fileupload.FileUploadReceiver;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.seb.sebserver.gui.service.push.ServerPushContext;
import ch.ethz.seb.sebserver.gui.service.push.ServerPushService;

public class ImageUpload extends Composite {

    private static final long serialVersionUID = 368264811155804533L;

    private static final Logger log = LoggerFactory.getLogger(ImageUpload.class);

    private transient final ServerPushService serverPushService;

    private final Composite imageCanvas;
    private final FileUpload fileUpload;
    private String imageBase64 = null;
    private boolean loadNewImage = false;
    private boolean imageLoaded = false;

    ImageUpload(final Composite parent, final ServerPushService serverPushService, final boolean readonly) {
        super(parent, SWT.NONE);
        super.setLayout(new GridLayout(1, false));

        this.serverPushService = serverPushService;

        if (!readonly) {
            this.fileUpload = new FileUpload(this, SWT.NONE);
            this.fileUpload.setImage(WidgetFactory.ImageIcon.IMPORT.getImage(parent.getDisplay()));
            this.fileUpload.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

            final FileUploadHandler uploadHandler = new FileUploadHandler(new FileUploadReceiver() {

                @Override
                public void receive(final InputStream stream, final FileDetails details) throws IOException {

                    try {
                        final String contentType = details.getContentType();
                        if (contentType != null && contentType.startsWith("image")) {
                            ImageUpload.this.imageBase64 = Base64.getEncoder()
                                    .encodeToString(IOUtils.toByteArray(stream));
                        }
                    } catch (final Exception e) {
                        log.error("Error while trying to upload image", e);
                    } finally {
                        ImageUpload.this.imageLoaded = true;
                        stream.close();
                    }
                }
            });

            this.fileUpload.addSelectionListener(new SelectionAdapter() {

                private static final long serialVersionUID = -6776734104137568801L;

                @Override
                public void widgetSelected(final SelectionEvent event) {
                    ImageUpload.this.loadNewImage = true;
                    ImageUpload.this.imageLoaded = false;
                    ImageUpload.this.fileUpload.submit(uploadHandler.getUploadUrl());

                    ImageUpload.this.serverPushService.runServerPush(
                            new ServerPushContext(ImageUpload.this, ImageUpload::uploadInProgress),
                            ImageUpload::wait,
                            ImageUpload::update);
                }
            });
        } else {
            this.fileUpload = null;
        }

        this.imageCanvas = new Composite(this, SWT.NONE);
        final GridData canvas = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.imageCanvas.setLayoutData(canvas);

    }

    public void setSelectionText(final String text) {
        if (this.fileUpload != null) {
            this.fileUpload.setToolTipText(text);
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

    private static final boolean uploadInProgress(final ServerPushContext context) {
        final ImageUpload imageUpload = (ImageUpload) context.getAnchor();
        return imageUpload.loadNewImage && !imageUpload.imageLoaded;
    }

    private static final void wait(final ServerPushContext context) {
        try {
            Thread.sleep(200);
        } catch (final InterruptedException e) {
            log.info("InterruptedException while wait for image uplaod. Just ignore it");
        }

    }

    private static final void update(final ServerPushContext context) {
        final ImageUpload imageUpload = (ImageUpload) context.getAnchor();
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
        }
    }

    private static void setImage(final ImageUpload imageUpload, final Base64InputStream input) {
        imageUpload.imageCanvas.setData(RWT.CUSTOM_VARIANT, "bgLogoNoImage");

        final Image image = new Image(imageUpload.imageCanvas.getDisplay(), input);
        final ImageData imageData = image.getImageData().scaledTo(200, 100);

        imageUpload.imageCanvas.setBackgroundImage(new Image(imageUpload.imageCanvas.getDisplay(), imageData));
    }

}
