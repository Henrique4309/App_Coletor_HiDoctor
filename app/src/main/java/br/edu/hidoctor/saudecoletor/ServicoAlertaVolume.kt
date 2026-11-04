package br.edu.hidoctor.saudecoletor

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class ServicoAlertaVolume : AccessibilityService() {

    private val detector = DetectorTriploVolume { Alertas.disparar(this, "acessibilidade") }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            detector.registrar()
        }
        return false // não consome: o volume continua subindo normalmente
    }
}