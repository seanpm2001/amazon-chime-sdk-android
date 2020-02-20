/*
 * Copyright (c) 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.amazon.chime.sdk.media.clientcontroller

import com.amazon.chime.sdk.media.enums.SignalStrength
import com.amazon.chime.sdk.media.enums.VolumeLevel
import com.amazon.chime.sdk.media.mediacontroller.AudioVideoObserver
import com.amazon.chime.sdk.media.mediacontroller.RealtimeObserver
import com.amazon.chime.sdk.session.MeetingSessionStatus
import com.amazon.chime.sdk.session.MeetingSessionStatusCode
import com.amazon.chime.sdk.session.SessionStateControllerAction
import com.amazon.chime.sdk.utils.logger.Logger
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class DefaultAudioClientObserverTest {
    @MockK
    private lateinit var mockLogger: Logger

    private lateinit var audioClientObserver: DefaultAudioClientObserver

    private var observerCalled = 0

    private val audioVideoObserver: AudioVideoObserver = object : AudioVideoObserver {
        override fun onAudioVideoStartConnecting(reconnecting: Boolean) {
            observerCalled += 1
        }

        override fun onAudioVideoStart(reconnecting: Boolean) {
            observerCalled += 1
        }

        override fun onAudioVideoStop(sessionStatus: MeetingSessionStatus) {
            observerCalled += 1
        }

        override fun onAudioReconnectionCancel() {
            observerCalled += 1
        }

        override fun onConnectionRecovered() {
            observerCalled += 1
        }

        override fun onConnectionBecamePoor() {
            observerCalled += 1
        }
    }

    private val realtimeObserver: RealtimeObserver = object : RealtimeObserver {
        override fun onVolumeChange(attendeeVolumes: Map<String, VolumeLevel>) {
            observerCalled += 1
        }

        override fun onSignalStrengthChange(attendeeSignalStrength: Map<String, SignalStrength>) {
            observerCalled += 1
        }
    }

    private val testObserverFun = { observer: AudioVideoObserver ->
        observer.onAudioVideoStartConnecting(
            false
        )
    }

    private val testProfileIds = arrayOf("aliceId", "bobId")

    // Used for both volume and signal
    private val testValues = IntArray(2) { it }

    private val mockUIThread = newSingleThreadContext("UI thread")

    @Before
    fun setup() {
        observerCalled = 0
        MockKAnnotations.init(this, relaxUnitFun = true)
        audioClientObserver = DefaultAudioClientObserver(mockLogger)
        audioClientObserver.subscribeToAudioClientStateChange(audioVideoObserver)
        audioClientObserver.subscribeToRealTimeEvents(realtimeObserver)
        Dispatchers.setMain(mockUIThread)
    }

    @After
    fun tearDown() {
        mockUIThread.close()
    }

    @Test
    fun `notifyAudioClientObserver should notify added observers`() {
        audioClientObserver.notifyAudioClientObserver(testObserverFun)

        Assert.assertEquals(1, observerCalled)
    }

    @Test
    fun `unsubscribeFromAudioClientStateChange should result in no notification`() {
        audioClientObserver.unsubscribeFromAudioClientStateChange(audioVideoObserver)

        audioClientObserver.notifyAudioClientObserver(testObserverFun)

        Assert.assertEquals(0, observerCalled)
    }

    @Test
    fun `onVolumeStateChange should notify added observers`() {
        audioClientObserver.onVolumeStateChange(testProfileIds, testValues)

        Assert.assertEquals(1, observerCalled)
    }

    @Test
    fun `onVolumeStateChange should NOT notify when no profile Ids`() {
        audioClientObserver.onVolumeStateChange(null, testValues)

        Assert.assertEquals(0, observerCalled)
    }

    @Test
    fun `onVolumeStateChange should NOT notify when no volumes`() {
        audioClientObserver.onVolumeStateChange(testProfileIds, null)

        Assert.assertEquals(0, observerCalled)
    }

    @Test
    fun `onSignalStrengthChange should notify added observers`() {
        audioClientObserver.onSignalStrengthChange(testProfileIds, testValues)

        Assert.assertEquals(1, observerCalled)
    }

    @Test
    fun `onSignalStrengthChange should notify when no profile Ids`() {
        audioClientObserver.onSignalStrengthChange(null, testValues)

        Assert.assertEquals(0, observerCalled)
    }

    @Test
    fun `onSignalStrengthChange should notify when no volumes`() {
        audioClientObserver.onSignalStrengthChange(testProfileIds, null)

        Assert.assertEquals(0, observerCalled)
    }

    @Test
    fun `unsubscribeFromRealTimeEvents should result in no notification`() {
        audioClientObserver.unsubscribeFromRealTimeEvents(realtimeObserver)

        audioClientObserver.onVolumeStateChange(testProfileIds, testValues)

        Assert.assertEquals(0, observerCalled)
    }

    @Test
    fun `onAudioClientStateChange should NOT notify when new state is UNKNOWN`() {
        audioClientObserver.onAudioClientStateChange(
            100,
            100
        )

        Assert.assertEquals(0, observerCalled)
    }

    @Test
    fun `onAudioClientStateChange should NOT notify when state and status is unchanged`() {
        audioClientObserver.onAudioClientStateChange(
            SessionStateControllerAction.Init.value,
            MeetingSessionStatusCode.OK.value
        )

        Assert.assertEquals(0, observerCalled)
    }
}
