/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.reactnativesip

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import com.facebook.react.ReactActivity
import org.linphone.core.*
import com.reactnativesip.SipModule

class OutgoingCallActivity: ReactActivity() {
    private var core: Core = SipModule.core

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.outgoing_call_activity)

        // For video to work, we need two TextureViews:
        // one for the remote video and one for the local preview
        core.nativeVideoWindowId = findViewById(R.id.remote_video_surface)
        // The local preview is a org.linphone.mediastream.video.capture.CaptureTextureView
        // which inherits from TextureView and contains code to keep the ratio of the capture video
        core.nativePreviewWindowId = findViewById(R.id.local_preview_video_surface)

        // Here we enable the video capture & display at Core level
        // It doesn't mean calls will be made with video automatically,
        // But it allows to use it later
        core.enableVideoCapture(true)
        core.enableVideoDisplay(true)

        // When enabling the video, the remote will either automatically answer the update request
        // or it will ask it's user depending on it's policy.
        // Here we have configured the policy to always automatically accept video requests
        core.videoActivationPolicy.automaticallyAccept = true
        // If you don't want to automatically accept,
        // you'll have to use a code similar to the one in toggleVideo to answer a received request

        // If the following property is enabled, it will automatically configure created call params with video enabled
        core.videoActivationPolicy.automaticallyInitiate = true

        findViewById<Button>(R.id.hang_up).isEnabled = true

        findViewById<Button>(R.id.hang_up).setOnClickListener {
            hangUp()
        }

        findViewById<Button>(R.id.openDoor).setOnClickListener {
            openDoorPress()
        }

        findViewById<Button>(R.id.toggle_video).setOnClickListener {
            toggleVideo()
        }

        findViewById<Button>(R.id.openDoor).isEnabled = true
        findViewById<Button>(R.id.toggle_video).isEnabled = true
        findViewById<Button>(R.id.hang_up).isEnabled = true
    }

    private fun hangUp() {
        if (core.callsNb == 0) return

        // If the call state isn't paused, we can get it using core.currentCall
        val call = if (core.currentCall != null) core.currentCall else core.calls[0]
        call ?: return

        // Terminating a call is quite simple
        call.terminate()
    }

    private fun toggleVideo() {
        if (core.callsNb == 0) return
        val call = if (core.currentCall != null) core.currentCall else core.calls[0]
        call ?: return

        // We will need the CAMERA permission for video call
        if (packageManager.checkPermission(Manifest.permission.CAMERA, packageName) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
            return
        }

        // To update the call, we need to create a new call params, from the call object this time

        val params = core.createCallParams(call)
        // Here we toggle the video state (disable it if enabled, enable it if disabled)
        // Note that we are using currentParams and not params or remoteParams
        // params is the object you configured when the call was started
        // remote params is the same but for the remote
        // current params is the real params of the call, resulting of the mix of local & remote params
        params?.enableVideo(!call.currentParams.videoEnabled())
        // Finally we request the call update
        call.update(params)

        // Note that when toggling off the video, TextureViews will keep showing the latest frame displayed
    }

    private fun openDoorPress() {
        if (core.callsNb == 0) return
        val call = if (core.currentCall != null) core.currentCall else core.calls[0]
        call ?: return

        var dtmf = "#"
        call?.sendDtmf(dtmf.single())
    }
}
