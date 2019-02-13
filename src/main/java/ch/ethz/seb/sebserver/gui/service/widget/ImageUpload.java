/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.widget;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.codec.binary.Base64InputStream;
import org.eclipse.rap.fileupload.FileDetails;
import org.eclipse.rap.fileupload.FileUploadHandler;
import org.eclipse.rap.fileupload.FileUploadReceiver;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.widgets.FileUpload;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
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

    private final ServerPushService serverPushService;

    final Composite imageCanvas;
    final FileUpload fileUpload;
    private String imageBase64 = null;
    private boolean loadNewImage = false;
    private boolean imageLoaded = false;

    ImageUpload(final Composite parent, final ServerPushService serverPushService) {
        super(parent, SWT.NONE);
        super.setLayout(new GridLayout(2, false));

        this.serverPushService = serverPushService;

        this.fileUpload = new FileUpload(this, SWT.NONE);
        this.fileUpload.setText("Select File");
        this.fileUpload.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

        this.imageCanvas = new Composite(this, SWT.NONE);
        final GridData canvas = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.imageCanvas.setLayoutData(canvas);

        final FileUploadHandler uploadHandler = new FileUploadHandler(new FileUploadReceiver() {

            @Override
            public void receive(final InputStream stream, final FileDetails details) throws IOException {
                try {
                    final String contentType = details.getContentType();
                    if (contentType != null && contentType.startsWith("image")) {
                        ImageUpload.this.imageBase64 = Base64.getEncoder().encodeToString(stream.readAllBytes());
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
                        new ServerPushContext(
                                ImageUpload.this,
                                runAgainContext -> {
                                    final ImageUpload imageUpload = (ImageUpload) runAgainContext.getAnchor();
                                    return imageUpload.loadNewImage && !imageUpload.imageLoaded;
                                }),
                        context -> {
                            try {
                                Thread.sleep(200);
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        },
                        context -> {
                            final ImageUpload imageUpload = (ImageUpload) context.getAnchor();
                            if (imageUpload.imageBase64 != null
                                    && imageUpload.loadNewImage
                                    && imageUpload.imageLoaded) {
                                final Base64InputStream input = new Base64InputStream(
                                        new ByteArrayInputStream(
                                                imageUpload.imageBase64.getBytes(StandardCharsets.UTF_8)),
                                        false);

                                imageUpload.imageCanvas.setData(RWT.CUSTOM_VARIANT, "bgLogoNoImage");
                                imageUpload.imageCanvas.setBackgroundImage(new Image(context.getDisplay(), input));
                                context.layout();
                                imageUpload.layout();
                                imageUpload.loadNewImage = false;
                            }
                        });
            }
        });

    }

    public String getImageBase64() {
        return this.imageBase64;
    }

    public void setImageBase64(final String imageBase64) {
        if (imageBase64 == null) {
            return;
        }

        this.imageBase64 = imageBase64;
        final Base64InputStream input = new Base64InputStream(
                new ByteArrayInputStream(imageBase64.getBytes(StandardCharsets.UTF_8)), false);
        this.imageCanvas.setData(RWT.CUSTOM_VARIANT, "bgLogoNoImage");
        this.imageCanvas.setBackgroundImage(new Image(super.getDisplay(), input));
    }

    public void setReadonly() {
        this.fileUpload.setVisible(false);
        this.fileUpload.setEnabled(false);
    }

}
