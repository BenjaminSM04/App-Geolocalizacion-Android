import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferences {

    private const val PREFS_FILENAME = "credencialesUsuario"

    fun getSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun guardarCredenciales(context: Context, email: String, password: String) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            putString("EMAIL", email)
            putString("PASSWORD", password)
            apply()
        }
    }

    fun obtenerCredenciales(context: Context): Pair<String?, String?> {
        val sharedPreferences = getSharedPreferences(context)
        val email = sharedPreferences.getString("EMAIL", null)
        val password = sharedPreferences.getString("PASSWORD", null)
        return Pair(email, password)
    }

    fun borrarCredenciales(context: Context) {
        val sharedPreferences = getSharedPreferences(context)
        with(sharedPreferences.edit()) {
            remove("EMAIL")
            remove("PASSWORD")
            apply()
        }
    }
}
