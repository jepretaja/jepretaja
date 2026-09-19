package com.jepretaja.app.data.model

object IndonesianLocations {
    val provinces: List<String> = listOf(
        "Aceh", "Bali", "Banten", "Bengkulu", "DI Yogyakarta", "DKI Jakarta",
        "Gorontalo", "Jambi", "Jawa Barat", "Jawa Tengah", "Jawa Timur",
        "Kalimantan Barat", "Kalimantan Selatan", "Kalimantan Tengah",
        "Kalimantan Timur", "Kalimantan Utara", "Kepulauan Bangka Belitung",
        "Kepulauan Riau", "Lampung", "Maluku", "Maluku Utara", "Nusa Tenggara Barat",
        "Nusa Tenggara Timur", "Papua", "Papua Barat", "Papua Barat Daya", "Papua Selatan", "Papua Tengah",
        "Papua Pegunungan", "Riau", "Sulawesi Barat", "Sulawesi Selatan",
        "Sulawesi Tengah", "Sulawesi Tenggara", "Sulawesi Utara", "Sumatera Barat",
        "Sumatera Selatan", "Sumatera Utara",
    )

    private val citiesByProvince: Map<String, List<String>> = mapOf(
        "Aceh" to listOf("Banda Aceh", "Langsa", "Lhokseumawe", "Meulaboh", "Sabang", "Sigli"),
        "Bali" to listOf("Denpasar", "Badung", "Bangli", "Buleleng", "Gianyar", "Jembrana", "Klungkung", "Tabanan"),
        "Banten" to listOf("Serang", "Cilegon", "Tangerang", "Tangerang Selatan", "Pandeglang", "Lebak", "Serang"),
        "Bengkulu" to listOf("Bengkulu", "Argamakmur", "Curup", "Mukomuko", "Rejang Lebong"),
        "DI Yogyakarta" to listOf("Yogyakarta", "Sleman", "Bantul", "Kulon Progo", "Gunungkidul"),
        "DKI Jakarta" to listOf("Jakarta Barat", "Jakarta Pusat", "Jakarta Selatan", "Jakarta Timur", "Jakarta Utara", "Kepulauan Seribu"),
        "Gorontalo" to listOf("Gorontalo", "Boalemo", "Bone Bolango", "Pohuwato"),
        "Jambi" to listOf("Jambi", "Sungai Penuh", "Muaro Jambi", "Batanghari"),
        "Jawa Barat" to listOf("Bandung", "Bekasi", "Bogor", "Cimahi", "Cirebon", "Depok", "Garut", "Karawang", "Tasikmalaya", "Sukabumi"),
        "Jawa Tengah" to listOf("Semarang", "Solo", "Magelang", "Pekalongan", "Tegal", "Purwokerto", "Salatiga"),
        "Jawa Timur" to listOf("Surabaya", "Malang", "Kediri", "Madiun", "Mojokerto", "Pasuruan", "Probolinggo", "Blitar"),
        "Kalimantan Barat" to listOf("Pontianak", "Singkawang", "Ketapang", "Sambas", "Sintang"),
        "Kalimantan Selatan" to listOf("Banjarmasin", "Banjarbaru", "Martapura", "Kotabaru", "Tanah Laut"),
        "Kalimantan Tengah" to listOf("Palangka Raya", "Sampit", "Kuala Kurun", "Muara Teweh"),
        "Kalimantan Timur" to listOf("Samarinda", "Balikpapan", "Bontang", "Tenggarong"),
        "Kalimantan Utara" to listOf("Tarakan", "Tanjung Selor", "Bulungan"),
        "Kepulauan Bangka Belitung" to listOf("Pangkal Pinang", "Tanjung Pandan", "Belitung", "Bangka"),
        "Kepulauan Riau" to listOf("Tanjungpinang", "Batam", "Bintan", "Karimun"),
        "Lampung" to listOf("Bandar Lampung", "Metro", "Pringsewu", "Lampung Selatan"),
        "Maluku" to listOf("Ambon", "Tual", "Masohi", "Saumlaki"),
        "Maluku Utara" to listOf("Ternate", "Tidore", "Soa Siu", "Morotai"),
        "Nusa Tenggara Barat" to listOf("Mataram", "Bima", "Sumbawa Besar", "Praya"),
        "Nusa Tenggara Timur" to listOf("Kupang", "Atambua", "Maumere", "Larantuka"),
        "Papua" to listOf("Jayapura", "Sentani", "Biak", "Merauke", "Timika"),
        "Papua Barat" to listOf("Manokwari", "Sorong", "Fakfak", "Kaimana"),
        "Papua Barat Daya" to listOf("Sorong", "Raja Ampat", "Sorong Selatan", "Maybrat", "Tambrauw"),
        "Papua Selatan" to listOf("Merauke", "Mulia", "Kepi"),
        "Papua Tengah" to listOf("Nabire", "Timika", "Wamena"),
        "Papua Pegunungan" to listOf("Wamena", "Jayawijaya", "Tolikara"),
        "Riau" to listOf("Pekanbaru", "Dumai", "Bangkinang", "Siak"),
        "Sulawesi Barat" to listOf("Mamuju", "Polewali Mandar", "Majene"),
        "Sulawesi Selatan" to listOf("Makassar", "Palopo", "Parepare", "Bulukumba", "Bone"),
        "Sulawesi Tengah" to listOf("Palu", "Donggala", "Banggai", "Poso"),
        "Sulawesi Tenggara" to listOf("Kendari", "Baubau", "Kolaka", "Unaaha"),
        "Sulawesi Utara" to listOf("Manado", "Bitung", "Tomohon", "Kotamobagu"),
        "Sumatera Barat" to listOf("Padang", "Bukittinggi", "Padang Panjang", "Solok"),
        "Sumatera Selatan" to listOf("Palembang", "Prabumulih", "Lubuklinggau", "Pagar Alam"),
        "Sumatera Utara" to listOf("Medan", "Pematangsiantar", "Binjai", "Tebing Tinggi", "Sibolga"),
    )

    fun citiesForProvince(province: String?): List<String> = citiesByProvince[province].orEmpty()
}
