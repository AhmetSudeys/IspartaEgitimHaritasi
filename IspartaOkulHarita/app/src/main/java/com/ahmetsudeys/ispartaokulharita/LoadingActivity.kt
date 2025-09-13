package com.ahmetsudeys.ispartaokulharita

import Okul
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoadingActivity : AppCompatActivity() {

    private val timeoutMs = 8_000L
    private val okulListesi = ArrayList<Okul>()
    private var navigated = false
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (!navigated) {
            Log.w("LoadingActivity", "Timeout: $timeoutMs ms doldu, eldeki veriyle devam ediliyor.")
            Toast.makeText(this, "Bağlantı yavaş, çevrimdışı açıldı.", Toast.LENGTH_SHORT).show()
            goToMaps(okulListesi)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Zaman aşımı
        timeoutHandler.postDelayed(timeoutRunnable, timeoutMs)

        val db = FirebaseFirestore.getInstance()
        db.collection("okullar")
            .get()
            .addOnSuccessListener { result ->
                try {
                    for (document in result) {
                        val ad = document.getString("ad") ?: continue
                        val enlem = document.getDouble("enlem") ?: continue
                        val boylam = document.getDouble("boylam") ?: continue

                        val okul = Okul(
                            ad = ad,
                            enlem = enlem,
                            boylam = boylam,
                            adres = document.getString("adres"),
                            ogrenciSayisi = document.getLong("ogrenciSayisi")?.toInt(),
                            ogretmenSayisi = document.getLong("ogretmenSayisi")?.toInt(),
                            derslikSayisi = document.getLong("derslikSayisi")?.toInt(),
                            kurumTuru = document.getString("kurumTuru"),
                            web = document.getString("web"),
                            telefon = document.getString("telefon"),
                            il = document.getString("il"),
                            ilce = document.getString("ilce")
                        )
                        okulListesi.add(okul)
                    }
                } catch (e: Exception) {
                    Log.e("FirestoreParse", "Doküman parse hatası", e)
                } finally {
                    goToMaps(okulListesi)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Koordinatlar alınamadı", e)
                Toast.makeText(this, "Veri alınamadı, çevrimdışı açıldı.", Toast.LENGTH_SHORT).show()
                goToMaps(okulListesi)
            }
    }

    private fun goToMaps(list: ArrayList<Okul>) {
        if (navigated) return
        navigated = true
        timeoutHandler.removeCallbacks(timeoutRunnable)

        val intent = Intent(this, MapsActivity::class.java).apply {
            putExtra("okulListesi", list)
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }
}
