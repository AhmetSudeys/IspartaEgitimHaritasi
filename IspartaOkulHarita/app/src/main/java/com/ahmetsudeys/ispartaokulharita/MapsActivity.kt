package com.ahmetsudeys.ispartaokulharita

import Okul
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ahmetsudeys.ispartaokulharita.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.util.Locale
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.view.WindowCompat


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private val markerListesi = mutableListOf<Marker>()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var filtrePanel: LinearLayout
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var okulListesi: ArrayList<Okul>
    private lateinit var adminPanel: LinearLayout
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnGirisYap: Button
    private lateinit var legendPanel: LinearLayout
    private lateinit var legendList: LinearLayout
    private var legendBuilt = false
    private lateinit var panel: View
    private lateinit var txtOkulAdi: TextView
    private lateinit var txtAdres: TextView
    private lateinit var txtIlce: TextView
    private lateinit var txtIl: TextView
    private lateinit var txtKurumTuru: TextView
    private lateinit var txtTelefon: TextView
    private lateinit var txtWeb: TextView
    private lateinit var txtOgrenci: TextView
    private lateinit var txtOgretmen: TextView
    private lateinit var txtDerslik: TextView
    private lateinit var btnYolTarifi: Button
    private var seciliKonum: LatLng? = null


    // TR-locale normalize: küçük harf + boşlukları sil
    private fun norm(raw: String?): String? =
        raw?.lowercase(Locale("tr", "TR"))?.replace("\\s+".toRegex(), "")?.trim()

    // Legend’deki sıralamayla birebir eşleştireceğiz
    private val ORDERED_TYPES = listOf(
        "Anaokulu",
        "İlkokul",
        "Ortaokul",
        "İmam Hatip Ortaokulu",
        "İmam Hatip Lisesi",
        "Anadolu İmam Hatip Lisesi",
        "Anadolu Lisesi",
        "Fen Lisesi",
        "Sosyal Bilimler Lisesi",
        "Güzel Sanatlar Lisesi",
        "Spor Lisesi",
        "Çok Programlı Anadolu Lisesi",
        "Anadolu Meslek Programı",
        "Anadolu Teknik Programı",
        "Mesleki Eğitim Merkezi",
        "Özel Eğitim Anaokulu",
        "Özel Eğitim Uygulama Okulu",
        "Özel Eğitim Meslek Okulu",
        "Rehberlik ve Araştırma Merkezi",
        "Halk Eğitim Merkezi",
        "BİLSEM",
        "İl Milli Eğitim Müdürlüğü",
        "İlçe Milli Eğitim Müdürlüğü",
        "Ölçme Değerlendirme Merkezi",
        "Hizmet İçi Eğitim Enstitüsü",
        "Akşam Sanat Okulu"
    )

    // Eşit aralıklı (14°) benzersiz renk tanımlamaları (0..360)
    private val DISTINCT_HUES = floatArrayOf(
        0f, 14f, 28f, 42f, 56f, 70f, 84f, 98f, 112f, 126f, 140f, 154f, 168f,
        182f, 196f, 210f, 224f, 238f, 252f, 266f, 280f, 294f, 308f, 322f, 336f, 350f
    )
    // Tür -> hue eşlemesi (normalize edilmiş anahtarlarla)
    private val TYPE_HUES: Map<String, Float> =
        ORDERED_TYPES.mapIndexed { idx, label ->
            norm(label)!! to DISTINCT_HUES[idx % DISTINCT_HUES.size]
        }.toMap()

    // Bilinmeyen tür olursa yine bu paletten deterministik bir seçim yap
    private fun hueForType(type: String?): Float {
        val key = norm(type) ?: return 0f
        return TYPE_HUES[key] ?: run {
            val i = kotlin.math.abs(key.hashCode()) % DISTINCT_HUES.size
            DISTINCT_HUES[i]
        }
    }

    // Google marker’ı için
    private fun iconForType(type: String?) =
        BitmapDescriptorFactory.defaultMarker(hueForType(type))

    // Legend noktaları için (HSV -> ARGB)
    private fun colorFromHue(hue: Float): Int {
        val hsv = floatArrayOf(hue, 0.9f, 0.95f) // doygun & parlak
        return Color.HSVToColor(hsv)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val btnLegend = findViewById<ImageButton>(R.id.btnLegend)
        legendPanel = findViewById(R.id.legend_panel)
        legendList = findViewById(R.id.legend_list)
        val btnCloseLegend = findViewById<ImageButton>(R.id.btnCloseLegend)

        btnLegend.setOnClickListener {
            if (legendPanel.visibility != View.VISIBLE) {
                buildLegendIfNeeded()
                legendPanel.visibility = View.VISIBLE
                legendPanel.bringToFront()
                legendPanel.alpha = 0f
                legendPanel.animate().alpha(1f).setDuration(120).start()
            } else {
                legendPanel.visibility = View.GONE
            }
        }
        btnCloseLegend.setOnClickListener { legendPanel.visibility = View.GONE }


        val btnCloseInfo = findViewById<ImageButton>(R.id.btnCloseInfo)
        btnCloseInfo.setOnClickListener {
            panel.visibility = View.GONE
        }

        adminPanel  = findViewById(R.id.adminPanel)
        etUsername  = findViewById(R.id.etUsername)
        etPassword  = findViewById(R.id.etPassword)
        btnGirisYap = findViewById(R.id.btnGirisYap)

        val btnCloseLogin = findViewById<ImageButton>(R.id.btnCloseLogin)
        btnCloseLogin.setOnClickListener {
            // paneli kapat + alanları temizle
            adminPanel.visibility = View.GONE
            etUsername.text?.clear()
            etPassword.text?.clear()
        }

        // Panel bileşenleri
        panel = findViewById(R.id.okul_info_panel)
        txtOkulAdi = findViewById(R.id.txtOkulAdi)
        txtAdres = findViewById(R.id.txtAdres)
        txtIlce = findViewById(R.id.txtIlce)
        txtIl = findViewById(R.id.txtIl)
        txtKurumTuru = findViewById(R.id.txtKurumTuru)
        txtTelefon = findViewById(R.id.txtTelefon)
        txtWeb = findViewById(R.id.txtWeb)
        txtOgrenci = findViewById(R.id.txtOgrenciSayisi)
        txtOgretmen = findViewById(R.id.txtOgretmenSayisi)
        txtDerslik = findViewById(R.id.txtDerslikSayisi)
        btnYolTarifi = findViewById(R.id.btnYolTarifi)

        // Okul verisini al
        okulListesi = intent.getSerializableExtra("okulListesi") as? ArrayList<Okul> ?: arrayListOf()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        filtrePanel = findViewById(R.id.filtre_panel)
        val btnCloseFilter = findViewById<ImageButton>(R.id.btnCloseFilter)
        btnCloseFilter.setOnClickListener { filtrePanel.visibility = View.GONE }
        val spinnerKurumTuru = findViewById<Spinner>(R.id.spinnerKurumTuru)
        val spinnerIlce = findViewById<Spinner>(R.id.spinnerIlce)
        val btnFiltrele = findViewById<Button>(R.id.btnFiltrele)
        val btnSifirla = findViewById<Button>(R.id.btnSifirla)

        // Firestore başlat
        firestore = FirebaseFirestore.getInstance()

        // Firestore'dan dinamik kurum türü ve ilçe listesi al
        val kurumTurleri = mutableSetOf<String>()
        val ilceler = mutableSetOf<String>()

        firestore.collection("okullar")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    doc.getString("kurumTuru")?.let { kurumTurleri.add(it) }
                    doc.getString("ilce")?.let { ilceler.add(it) }
                }

                val kurumTurListesi = listOf("Hepsi") + kurumTurleri.sorted()
                val ilceListesi = listOf("Hepsi") + ilceler.sorted()

                val kurumAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, kurumTurListesi)
                val ilceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ilceListesi)

                spinnerKurumTuru.adapter = kurumAdapter
                spinnerIlce.adapter = ilceAdapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Veriler alınamadı", Toast.LENGTH_SHORT).show()
            }

        // Filtre butonu
        btnFiltrele.setOnClickListener {
            val secilenTur = spinnerKurumTuru.selectedItem.toString().trim()
            val secilenIlce = spinnerIlce.selectedItem.toString().trim()

            // Eski markerları temizle
            for (m in markerListesi) m.remove()
            markerListesi.clear()

            // Null güvenli ve büyük/küçük harf duyarsız filtre
            val filtrelenmisOkullar = okulListesi.filter { okul ->
                val tur   = okul.kurumTuru?.trim()
                val ilce  = okul.ilce?.trim()

                val turUyar = (secilenTur == "Hepsi") || (tur?.equals(secilenTur, ignoreCase = true) == true)
                val ilceUyar = (secilenIlce == "Hepsi") || (ilce?.equals(secilenIlce, ignoreCase = true) == true)

                turUyar && ilceUyar
            }

            // Yeni markerları ekle
            for (okul in filtrelenmisOkullar) {
                val konum = LatLng(okul.enlem, okul.boylam)
                mMap.addMarker(
                    MarkerOptions()
                        .position(konum)
                        .title(okul.ad)
                        .icon(iconForType(okul.kurumTuru))
                )?.let { marker ->
                    marker.tag = okul
                    markerListesi.add(marker)
                }

            }

            val ilceKonumlari = mapOf(
                "MERKEZ" to LatLng(37.7648, 30.5566),
                "YALVAÇ" to LatLng(38.2952, 31.1775),
                "GELENDOST" to LatLng(38.1222, 31.0178),
                "EĞİRDİR" to LatLng(37.8747, 30.8541),
                "AKSU" to LatLng(37.799483, 31.071275),
                "ATABEY" to LatLng(37.9500, 30.6333),
                "KEÇİBORLU" to LatLng(37.9411, 30.3000),
                "SENİRKENT" to LatLng(38.0864, 30.5522),
                "SÜTÇÜLER" to LatLng(37.4986, 30.9828),
                "ŞARKİKARAAĞAÇ" to LatLng(38.0783, 31.3667),
                "ULUBORLU" to LatLng(38.0803, 30.4553),
                "YENİŞARBADEMLİ" to LatLng(37.709497, 31.390155),
                "GÖNEN" to LatLng(37.956481, 30.511802)
            )

            // Seçilen ilçeye göre haritayı konumlandır
            ilceKonumlari[secilenIlce]?.let {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 13f))
            }

            if (filtrelenmisOkullar.isEmpty()) {
                Toast.makeText(this, "Bu filtreye uygun okul bulunamadı.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Filtre: $secilenTur - $secilenIlce", Toast.LENGTH_SHORT).show()
            }

            filtrePanel.visibility = View.GONE
        }

        btnSifirla.setOnClickListener {
            spinnerKurumTuru.setSelection(0)
            spinnerIlce.setSelection(0)

            // Tüm markerları temizle
            for (m in markerListesi) m.remove()
            markerListesi.clear()

            // Tüm okulları tekrar göster
            for (okul in okulListesi) {
                val konum = LatLng(okul.enlem, okul.boylam)
                mMap.addMarker(
                    MarkerOptions()
                        .position(konum)
                        .title(okul.ad)
                        .icon(iconForType(okul.kurumTuru))
                )?.let { marker ->
                    marker.tag = okul
                    markerListesi.add(marker)
                }
            }

            val ispartaMerkez = LatLng(37.7648, 30.5566)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ispartaMerkez, 12f))

            Toast.makeText(this, "Filtre sıfırlandı", Toast.LENGTH_SHORT).show()
            filtrePanel.visibility = View.GONE
        }

        btnGirisYap.setOnClickListener {
            val kullanici = etUsername.text.toString()
            val sifre = etPassword.text.toString()
            if (kullanici == "admin" && sifre == "1234") {
                etUsername.text?.clear()
                etPassword.text?.clear()

                adminPanel.visibility = View.GONE
                showAdminDialog()
                Toast.makeText(this, "Giriş başarılı", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Hatalı kullanıcı adı veya şifre", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val ispartaMerkez = LatLng(37.7648, 30.5566)
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ispartaMerkez, 12f))

        for (okul in okulListesi) {
            val konum = LatLng(okul.enlem, okul.boylam)
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(konum)
                    .title(okul.ad)
                    .icon(iconForType(okul.kurumTuru))
            )

            marker?.tag = okul
            if (marker != null) {
                markerListesi.add(marker)
            }
        }

        mMap.setOnMarkerClickListener { marker ->
            val okulAdi = marker.title
            firestore.collection("okullar")
                .whereEqualTo("ad", okulAdi)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc = documents.first()

                        txtOkulAdi.text = okulAdi
                        txtAdres.text = "Adres: ${doc.getString("adres") ?: "Bilgi bulunmamaktadır."}"
                        txtIlce.text = "İlçe: ${doc.getString("ilce") ?: "Bilgi bulunmamaktadır."}"
                        txtIl.text = "İl: ${doc.getString("il") ?: "Bilgi bulunmamaktadır."}"
                        txtKurumTuru.text = "Kurum Türü: ${doc.getString("kurumTuru") ?: "Bilgi bulunmamaktadır."}"
                        txtTelefon.text = "Telefon: ${doc.getString("telefon") ?: "Bilgi bulunmamaktadır."}"
                        txtWeb.text = "Web: ${doc.getString("web") ?: "Bilgi bulunmamaktadır."}"
                        txtOgrenci.text = "Öğrenci Sayısı: ${doc.getLong("ogrenciSayisi") ?: 0}"
                        txtOgretmen.text = "Öğretmen Sayısı: ${doc.getLong("ogretmenSayisi") ?: 0}"
                        txtDerslik.text = "Derslik Sayısı: ${doc.getLong("derslikSayisi") ?: 0}"

                        seciliKonum = marker.position
                        panel.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(this, "Okul verisi bulunamadı.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Veri alınırken hata oluştu.", Toast.LENGTH_SHORT).show()
                }

            true
        }

        btnYolTarifi.setOnClickListener {
            seciliKonum?.let {
                val gmmIntentUri = Uri.parse("google.navigation:q=${it.latitude},${it.longitude}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                startActivity(mapIntent)
            }
        }

        googleMap.setOnMapClickListener {
            binding.okulInfoPanel.visibility = View.GONE
            filtrePanel.visibility = View.GONE
            adminPanel.visibility = View.GONE
            legendPanel.visibility = View.GONE

        }

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)

        var justOpenedAdmin = false

        btnMenu.setOnClickListener {
            val popup = androidx.appcompat.widget.PopupMenu(this, btnMenu)
            popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_admin -> {
                        binding.okulInfoPanel.visibility = View.GONE
                        filtrePanel.visibility = View.GONE
                        etUsername.text?.clear()
                        etPassword.text?.clear()

                        justOpenedAdmin = true
                        adminPanel.visibility = View.VISIBLE
                        adminPanel.bringToFront()
                        adminPanel.alpha = 0f
                        adminPanel.animate().alpha(1f).setDuration(150).start()

                        // 200ms sonra korumayı kapat
                        adminPanel.postDelayed({ justOpenedAdmin = false }, 200)
                        true
                    }
                    R.id.menu_filtrele -> {
                        adminPanel.visibility = View.GONE
                        binding.okulInfoPanel.visibility = View.GONE
                        filtrePanel.visibility =
                            if (filtrePanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        mMap.setOnMapClickListener {
            if (justOpenedAdmin) return@setOnMapClickListener
            binding.okulInfoPanel.visibility = View.GONE
            filtrePanel.visibility = View.GONE
            adminPanel.visibility = View.GONE
        }

    }

    private fun showAdminDialog() {
        // 1) View'u şişir
        val dialogView = layoutInflater.inflate(R.layout.admin_actions_dialog, null)

        // View referansları
        val etOkulAra = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.etOkulAra)
        val etAdres    = dialogView.findViewById<EditText>(R.id.etAdres)
        val etTelefon  = dialogView.findViewById<EditText>(R.id.etTelefon)
        val etWeb      = dialogView.findViewById<EditText>(R.id.etWeb)
        val etKurumTuru= dialogView.findViewById<EditText>(R.id.etKurumTuru)
        val etOgrenci2 = dialogView.findViewById<EditText>(R.id.etOgrenciSayisi2)
        val etOgretmen2= dialogView.findViewById<EditText>(R.id.etOgretmenSayisi2)
        val etDerslik2 = dialogView.findViewById<EditText>(R.id.etDerslikSayisi2)
        val btnGuncelle2 = dialogView.findViewById<Button>(R.id.btnGuncelle2)
        val btnSil       = dialogView.findViewById<Button>(R.id.btnSil)
        val btnKapat     = dialogView.findViewById<ImageButton>(R.id.btnCloseDialog)

        // 2) Dialog'u oluştur
        val dlg = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create().apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }

        // 3) Okul isimlerini çek + AutoComplete'e bağla
        val okulAdlari = mutableListOf<String>()
        val adToDocId  = mutableMapOf<String, String>()
        var secilenDocId: String? = null

        fun doldur(docId: String) {
            firestore.collection("okullar").document(docId).get()
                .addOnSuccessListener { doc ->
                    secilenDocId = doc.id
                    etAdres.setText(doc.getString("adres") ?: "")
                    etTelefon.setText(doc.getString("telefon") ?: "")
                    etWeb.setText(doc.getString("web") ?: "")
                    etKurumTuru.setText(doc.getString("kurumTuru") ?: "")
                    etOgrenci2.setText((doc.getLong("ogrenciSayisi") ?: 0).toString())
                    etOgretmen2.setText((doc.getLong("ogretmenSayisi") ?: 0).toString())
                    etDerslik2.setText((doc.getLong("derslikSayisi") ?: 0).toString())
                }
        }

        firestore.collection("okullar").get()
            .addOnSuccessListener { docs ->
                for (d in docs) {
                    val ad = d.getString("ad") ?: continue
                    okulAdlari.add(ad)
                    adToDocId[ad] = d.id
                }
                okulAdlari.sort()

                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, okulAdlari)
                etOkulAra.setAdapter(adapter)

                etOkulAra.threshold = 0
                etOkulAra.setOnClickListener { etOkulAra.showDropDown() }
                etOkulAra.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) etOkulAra.showDropDown()
                }

                // Listeden seçilince doldur
                etOkulAra.setOnItemClickListener { parent, _, position, _ ->
                    val ad = parent.getItemAtPosition(position) as String
                    adToDocId[ad]?.let { doldur(it) }
                }

                etOkulAra.setOnDismissListener {
                    adToDocId[etOkulAra.text.toString()]?.let { doldur(it) }
                }
            }

        // 4) Güncelle
        btnGuncelle2.setOnClickListener {
            val id = secilenDocId
            if (id == null) {
                Toast.makeText(this, "Önce okul seçiniz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val updateMap = mapOf(
                "adres"          to etAdres.text.toString(),
                "telefon"        to etTelefon.text.toString(),
                "web"            to etWeb.text.toString(),
                "kurumTuru"      to etKurumTuru.text.toString(),
                "ogrenciSayisi"  to (etOgrenci2.text.toString().toIntOrNull() ?: 0),
                "ogretmenSayisi" to (etOgretmen2.text.toString().toIntOrNull() ?: 0),
                "derslikSayisi"  to (etDerslik2.text.toString().toIntOrNull() ?: 0)
            )
            firestore.collection("okullar").document(id).update(updateMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Bilgiler güncellendi.", Toast.LENGTH_SHORT).show()
                    dlg.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Güncelleme başarısız.", Toast.LENGTH_SHORT).show()
                }
        }

        // 5) Sil
        btnSil.setOnClickListener {
            val id = secilenDocId
            if (id == null) {
                Toast.makeText(this, "Önce okul seçiniz.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Okulu Sil")
                .setMessage("Bu okulu kalıcı olarak silmek istiyor musunuz?")
                .setPositiveButton("Sil") { _, _ ->
                    firestore.collection("okullar").document(id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Okul silindi.", Toast.LENGTH_SHORT).show()
                            dlg.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Silme başarısız.", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Vazgeç", null)
                .show()
        }

        btnKapat.setOnClickListener { dlg.dismiss() }

        dlg.show()
        dlg.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }


    private fun makeDot(color: Int, sizeDp: Int = 12): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(color)
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
        d.setSize(sizePx, sizePx)
        return d
    }

    private fun buildLegendIfNeeded() {
        if (legendBuilt) return

        val items = listOf(
            "Anaokulu",
            "İlkokul",
            "Ortaokul",
            "İmam Hatip Ortaokulu",
            "İmam Hatip Lisesi",
            "Anadolu İmam Hatip Lisesi",
            "Anadolu Lisesi",
            "Fen Lisesi",
            "Sosyal Bilimler Lisesi",
            "Güzel Sanatlar Lisesi",
            "Spor Lisesi",
            "Çok Programlı Anadolu Lisesi",
            "Anadolu Meslek Programı",
            "Anadolu Teknik Programı",
            "Mesleki Eğitim Merkezi",
            "Özel Eğitim Anaokulu",
            "Özel Eğitim Uygulama Okulu",
            "Özel Eğitim Meslek Okulu",
            "Rehberlik ve Araştırma Merkezi",
            "Halk Eğitim Merkezi",
            "BİLSEM",
            "İl Milli Eğitim Müdürlüğü",
            "İlçe Milli Eğitim Müdürlüğü",
            "Ölçme Değerlendirme Merkezi",
            "Hizmet İçi Eğitim Enstitüsü",
            "Akşam Sanat Okulu"
        )

        val dp = resources.displayMetrics.density
        items.forEach { label ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
            }

            val dotView = View(this).apply {
                val lp = LinearLayout.LayoutParams((14 * dp).toInt(), (14 * dp).toInt())
                lp.rightMargin = (8 * dp).toInt()
                layoutParams = lp
                background = makeDot(colorFromHue(hueForType(label)))
            }

            val tv = TextView(this).apply {
                text = label
                setTextColor(Color.parseColor("#333333"))
                textSize = 14f
            }

            row.addView(dotView)
            row.addView(tv)
            legendList.addView(row)
        }

        legendBuilt = true
    }
}

