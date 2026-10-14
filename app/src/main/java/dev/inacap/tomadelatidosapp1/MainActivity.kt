package dev.inacap.tomadelatidosapp1

import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import android.os.Bundle
import android.widget.TextView
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : AppCompatActivity() {

    private lateinit var tvBpm: TextView
    private lateinit var tvEstado: TextView
    private val topic = "inacap/proyecto/pulsos" // El mismo tópico de tu Arduino

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBpm = findViewById(R.id.tvBpm)
        tvEstado = findViewById(R.id.tvEstado)

        conectarMQTT()
    }

    private fun conectarMQTT() {
        // Los procesos de red deben ir en un hilo secundario
        Thread {
            try {
                val clientId = MqttClient.generateClientId()
                val client = MqttClient("tcp://broker.hivemq.com:1883", clientId, MemoryPersistence())
                val options = MqttConnectOptions()
                options.isCleanSession = true

                client.setCallback(
                    object : MqttCallback {
                        override fun connectionLost(cause: Throwable?) {
                        runOnUiThread { tvEstado.text = getString(R.string.status_disconnected) }
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val bpmRecibido = message.toString().toIntOrNull() ?: 0

                        // Actualizar la interfaz gráfica (UI)
                        runOnUiThread {
                            if (bpmRecibido == 0) {
                                tvBpm.text = "0"
                                tvEstado.text = getString(R.string.status_no_finger)
                                tvEstado.background.setTint("#9E9E9E".toColorInt())
                            } else {
                                tvBpm.text = bpmRecibido.toString()
                                if (bpmRecibido > 100) {
                                    tvEstado.text = getString(R.string.status_high_alert)
                                    tvEstado.background.setTint("#D32F2F".toColorInt())
                                } else {
                                    tvEstado.text = getString(R.string.status_normal)
                                    tvEstado.background.setTint("#388E3C".toColorInt())
                                }
                            }
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                }
                )

                client.connect(options)
                client.subscribe(topic)

                runOnUiThread { tvEstado.text = getString(R.string.status_connected) }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { tvEstado.text = getString(R.string.status_error) }
            }
        }.start()
    }
}