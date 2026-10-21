package com.catalogoapp.consultoras.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.catalogoapp.consultoras.R
import com.catalogoapp.consultoras.data.repository.PedidoRepository
import com.google.firebase.auth.FirebaseAuth

class RecordatorioPedidoWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = PedidoRepository()

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        val resultado = repository.listarPendientesAntiguos(DIAS_LIMITE)
        return resultado.fold(
            onSuccess = { pedidosOlvidados ->
                if (pedidosOlvidados.isNotEmpty()) {
                    notificar(pedidosOlvidados.size)
                }
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }

    private fun notificar(cantidadPedidos: Int) {
        val canalId = "recordatorios_pedidos"
        val administrador = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(canalId, "Recordatorios de pedidos", NotificationManager.IMPORTANCE_DEFAULT)
            administrador.createNotificationChannel(canal)
        }
        val notificacion = NotificationCompat.Builder(applicationContext, canalId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Tienes pedidos pendientes de seguimiento")
            .setContentText("$cantidadPedidos pedido(s) llevan más de $DIAS_LIMITE días en estado PENDIENTE")
            .setAutoCancel(true)
            .build()
        administrador.notify(ID_NOTIFICACION, notificacion)
    }

    companion object {
        const val NOMBRE_TRABAJO = "recordatorio_pedidos"
        private const val DIAS_LIMITE = 3
        private const val ID_NOTIFICACION = 5001
    }
}
