package dev.inacap.tomadelatidosapp1

import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : AppCompatActivity() {

    private lateinit var tvBpm: TextView
    private lateinit var tvEstado: TextView
    private lateinit var btnSimZero: Button
    private lateinit var btnSimNormal: Button
    private lateinit var btnSimAlert: Button

    private var mqttClient: MqttClient? = null
    private val topic = "inacap/proyecto/pulsos" // Tópico MQTT compartido entre dispositivos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBpm = findViewById(R.id.tvBpm)
        tvEstado = findViewById(R.id.tvEstado)
        btnSimZero = findViewById(R.id.btnSimZero)
        btnSimNormal = findViewById(R.id.btnSimNormal)
        btnSimAlert = findViewById(R.id.btnSimAlert)

        // Configurar botones del Simulador / Controlador IoT
        btnSimZero.setOnClickListener { publicaBpmSimulado(0) }
        btnSimNormal.setOnClickListener { publicaBpmSimulado(75) }
        btnSimAlert.setOnClickListener { publicaBpmSimulado(130) }

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
                options.connectionTimeout = 10
                options.keepAliveInterval = 60

                client.setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        runOnUiThread {
                            tvEstado.text = getString(R.string.status_disconnected)
                            tvEstado.background.setTint("#9E9E9E".toColorInt())
                        }
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        val bpmRecibido = message.toString().toIntOrNull() ?: 0

                        // Actualizar la interfaz gráfica (UI) en tiempo real al recibir datos
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
                })

                client.connect(options)
                client.subscribe(topic)
                mqttClient = client

                runOnUiThread {
                    tvEstado.text = getString(R.string.status_connected)
                    tvEstado.background.setTint("#388E3C".toColorInt()) // Verde al conectar con éxito
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvEstado.text = getString(R.string.status_error)
                    tvEstado.background.setTint("#D32F2F".toColorInt()) // Rojo en caso de error
                }
            }
        }.start()
    }

    // Función para enviar comandos/datos simulados desde este celular hacia los demás
    private fun publicaBpmSimulado(bpm: Int) {
        Thread {
            try {
                if (mqttClient != null && mqttClient!!.isConnected) {
                    val message = MqttMessage(bpm.toString().toByteArray())
                    mqttClient?.publish(topic, message)
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Aún no hay conexión al servidor MQTT", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error al enviar datos simulados", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}