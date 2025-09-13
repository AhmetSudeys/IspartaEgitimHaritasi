import java.io.Serializable

data class Okul(
    val ad: String,
    val enlem: Double,
    val boylam: Double,
    val adres: String?,
    val ogrenciSayisi: Int?,
    val ogretmenSayisi: Int?,
    val derslikSayisi: Int?,
    val kurumTuru: String?,
    val web: String?,
    val telefon: String?,
    val il: String?,
    val ilce: String?
) : Serializable
