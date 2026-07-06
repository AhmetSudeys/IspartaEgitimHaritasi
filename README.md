# 🗺️ Isparta Eğitim Haritası

**Isparta Eğitim Haritası**, Isparta ilindeki Millî Eğitim Bakanlığına bağlı tüm eğitim kurumlarını harita üzerinde gösteren Android tabanlı bir mobil uygulamadır.  
Bu proje, staj süreci kapsamında geliştirilmiştir.

---

## 📌 Özellikler

- 📍 **Harita Entegrasyonu (Google Maps SDK)**  
  Tüm kurumlar harita üzerinde marker olarak gösterilir.

- 🏫 **Detaylı Kurum Bilgileri**  
  Marker'a tıklanınca açılan panelde şu bilgiler gösterilir:
  - Okul adı  
  - Adres  
  - İl / İlçe  
  - Kurum türü  
  - Telefon  
  - Web sitesi  
  - Öğrenci sayısı  
  - Öğretmen sayısı  
  - Derslik sayısı

- ⚡ **Performans Odaklı Yükleme (Lazy Fetch)**  
  Uygulama açılışta yalnızca konum (enlem–boylam) verilerini yükler, detaylar marker tıklanınca getirilir.

- 🎨 **Modern ve Kullanıcı Dostu Arayüz**  
  Estetik ve sade tasarlanmış arayüz bileşenleri ile kolay kullanım.

- 🔍 **Dinamik Filtreleme Sistemi**
  - İlçe ve kurum türüne göre filtreleme
  - Filtreyi sıfırlama
  - Filtre içerikleri Firestore’dan dinamik olarak alınır

- 🛠️ **Yönetici (Admin) Paneli**
  - Yönetici girişi sonrası:
    - Kurum güncelleme
    - Kurum silme
  - Doğrudan Firestore ile entegre çalışır

---

## 🛠️ Kullanılan Teknolojiler

- Kotlin (Android)
- Firebase Firestore (Bulut veritabanı)
- Google Maps SDK
- Material Design UI Bileşenleri

---

## 📲 Uygulamayı İndirme

📦 [.apk dosyasını buradan indirebilirsiniz](<img width="718" height="723" alt="qr" src="https://github.com/user-attachments/assets/6623b74b-8743-4f9d-bf03-6b7d088c63c6" />
)

---

## 📸 Görseller

<table border="0">
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/1c02d07e-d5cc-4cf5-9635-57f2862c4896" width="300px"/><br/>
      <sub><b>Harita Ana Görünüm</b></sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/088afb97-a681-43ab-8501-8bfa14cabceb" width="300px"/><br/>
      <sub><b>Arama ve Filtreleme</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/69252062-273c-4d0a-a126-13c30c41b3df" width="300px"/><br/>
      <sub><b>Kurum Detay Paneli</b></sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/ca744626-fd36-4beb-804d-3cfe1da0e268" width="300px"/><br/>
      <sub><b>İlçe Seçimi</b></sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/9a4bf898-5116-4b74-ad77-6c607f700319" width="300px"/><br/>
      <sub><b>Admin Giriş Ekranı</b></sub>
    </td>
    <td align="center">
      <img src="https://github.com/user-attachments/assets/06de0936-6759-497b-8753-d95f38cf1ffe" width="300px"/><br/>
      <sub><b>Veri Güncelleme Paneli</b></sub>
    </td>
  </tr>
</table>







