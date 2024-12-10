package space.thatnawfal.iotattendance

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.icu.util.Calendar
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import org.json.JSONObject
import space.thatnawfal.iotattendance.data.User
import space.thatnawfal.iotattendance.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isDatePickerVisibile: Boolean = false
    private var nfcAdapter: NfcAdapter? = null

    private var rfid: String? = null
    private var name: String? = null
    private var nohp: String? = null
    private var date: String? = null
    private var keyList : ArrayList<String> = ArrayList()

    @SuppressLint("SetTextI18n", "NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        getExcistKey()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        binding.btnDate.setOnClickListener {
            if (!isDatePickerVisibile) {
                binding.viewDatepicker.visibility = View.VISIBLE
                binding.dpExpired.visibility = View.VISIBLE
            }
        }

        val datePicker = binding.dpExpired

        datePicker.setOnDateChangedListener { _, year, month, day ->
            val selectedDate = "$day/${month + 1}/$year"
            binding.tvExpired.text = "Expired : $selectedDate"
            binding.tvExpired.setTextColor(ActivityCompat.getColor(this, R.color.black))
            isDatePickerVisibile = false
            date = selectedDate
            binding.viewDatepicker.visibility = View.INVISIBLE
            binding.dpExpired.visibility = View.INVISIBLE
        }

        binding.btnSubmit.setOnClickListener {
//            Log.d("FILL TEXT:", "onCreate: {${binding.etName.text}, ${binding.etName.text?.isEmpty()}}")
            if (rfid != null && name != null && nohp != null && date != null){
                Log.d("Firebase", "Step 1")
                if (!rfidRegistered()){
                    Log.d("Firebase", "Step 2")
                    registerUser()
                }else {
                    binding.tvIdRfid.text = "RFID $rfid Sudah Terdaftar"
                    binding.tvIdRfid.setTextColor(ContextCompat.getColor(this, R.color.red))
                }

            } else {
                if (rfid == null){
                    binding.tvIdRfid.text = "Scan Rfid Dulu"
                    binding.tvIdRfid.setTextColor(ContextCompat.getColor(this, R.color.red))
                }

                if (binding.etName.text?.isEmpty() == true){
                    binding.etName.error = "Nama Harus Diisi"
                } else if (binding.etName.text?.isEmpty() == false){
                    name = binding.etName.text.toString()
                }

                if (binding.etHp.text?.isEmpty() == true){
                    binding.etHp.error = "Nomor HP Harus Diisi"
                } else if (binding.etHp.text?.isEmpty() == false){
                    nohp = binding.etHp.text.toString()
                }

                if (date == null){
                    binding.tvExpired.text = "Tanggal Harus Diisi"
                    binding.tvExpired.setTextColor(ContextCompat.getColor(this, R.color.red))
                }
            }
        }

    }

    private fun getExcistKey() {
        val database = Firebase.database
        val rfidRef = database.getReference("")

        rfidRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val hashMapValue = snapshot.getValue<HashMap<String, Any>>()
                for ((key, values) in hashMapValue!!) {
                    keyList.add(key)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }

    private fun rfidRegistered(): Boolean {

        var isRegistered = true

        if (rfid?.contains(rfid!!) == true){
            isRegistered = false
        }

        return isRegistered
    }

    override fun onResume() {
        super.onResume()

        val pendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE)

        val intentFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val filters = arrayOf(intentFilter)

        // Start listening for NFC tags
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    @SuppressLint("SetTextI18n")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let { super.onNewIntent(it) }

        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

            if (tag != null) {
                val tagId = tag.id.joinToString("") { "%02x".format(it) }
                Log.d("NFC-", "onNewIntent: $tagId")
                rfid = tagId
                binding.tvIdRfid.text = "RFID : $tagId"
                binding.tvIdRfid.setTextColor(ContextCompat.getColor(this, R.color.black))

            }
        }
    }

    private fun registerUser() {
        Log.d("Firebase", "Register User")

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val createdDate = "$day/${month + 1}/$year"

        val database = Firebase.database
        val myRef = database.getReference("")

        val data = User(
            active = 0,
            name = name,
            phone_number = nohp,
            created_at = createdDate,
            updated_at = createdDate,
            expired_at = date
        )

        myRef.child(rfid!!).setValue(data).addOnSuccessListener {
            Log.d("Firebase REG:", "Data sent successfully")
        }.addOnFailureListener {
            Log.d("Firebase", "Data failed to send")
        }
        finish()
    }


}