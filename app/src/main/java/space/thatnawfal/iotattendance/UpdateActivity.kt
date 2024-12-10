package space.thatnawfal.iotattendance

import android.annotation.SuppressLint
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import space.thatnawfal.iotattendance.databinding.ActivityUpdateBinding

class UpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateBinding
    private var isDatePickerVisibile: Boolean = false

    private var inputRfid: String? = null
    private var inputName: String? = null
    private var inputNohp: String? = null
    private var inputExpired: String? = null
    private var inputCreated: String? = null
    private var inputUpdated: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        updateField()
        binding.btnDate.setOnClickListener {
            if (!isDatePickerVisibile) {
                binding.viewDatepicker.visibility = View.VISIBLE
                binding.dpExpired.visibility = View.VISIBLE
            }
        }
        datePickerFunction()

        binding.btnSubmit.setOnClickListener {
            updateMember()
        }

    }

    private fun updateMember() {
        if (inputRfid != null && inputName != null && inputNohp != null && inputExpired != null) {
            Log.d("Firebase", "Step 1")
            updateFirebase()
        } else {
            if (inputRfid == null){
                binding.tvIdRfid.text = "Scan Rfid Dulu"
                binding.tvIdRfid.setTextColor(ContextCompat.getColor(this, R.color.red))
            }

            if (binding.etName.text?.isEmpty() == true){
                binding.etName.error = "Nama Harus Diisi"
            } else if (binding.etName.text?.isEmpty() == false){
                inputName = binding.etName.text.toString()
            }

            if (binding.etHp.text?.isEmpty() == true){
                binding.etHp.error = "Nomor HP Harus Diisi"
            } else if (binding.etHp.text?.isEmpty() == false){
                inputNohp = binding.etHp.text.toString()
            }

            if (inputExpired == null){
                binding.tvExpired.text = "Tanggal Harus Diisi"
                binding.tvExpired.setTextColor(ContextCompat.getColor(this, R.color.red))
            }
        }
    }

    private fun updateFirebase() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val createdDate = "$day/${month}/$year"

        val database = Firebase.database
        val myRef = database.getReference()

        val data = hashMapOf(
            "active" to 0,
            "name" to binding.etName.text.toString(),
            "phone_number" to binding.etHp.text.toString(),
            "created_at" to inputCreated,
            "updated_at" to createdDate,
            "expired_at" to inputExpired
        )

        myRef.child(inputRfid!!).setValue(data).addOnFailureListener {
            Log.d("Firebase", "Data failed to send")
        }.addOnSuccessListener {
            Log.d("Firebase REG:", "Data sent successfully")
            finish()
        }
    }

    @SuppressLint("NewApi", "SetTextI18n")
    private fun datePickerFunction() {
        binding.dpExpired.setOnDateChangedListener { _, year, month, day ->
            val selectedDate = "$day/${month + 1}/$year"
            binding.tvExpired.text = "Expired : $selectedDate"
            binding.tvExpired.setTextColor(ActivityCompat.getColor(this, R.color.black))
            isDatePickerVisibile = false
            inputExpired = selectedDate
            binding.viewDatepicker.visibility = View.INVISIBLE
            binding.dpExpired.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateField() {
        val rfid = intent.getStringExtra("rfid")
        val name = intent.getStringExtra("name")
        val phone = intent.getStringExtra("phone")
        val expired = intent.getStringExtra("expired")
        val created = intent.getStringExtra("created")
        val updated = intent.getStringExtra("updated")

        inputRfid = rfid
        inputName = name
        inputNohp = phone
        inputExpired = expired
        inputCreated = created
        inputUpdated = updated

        binding.tvIdRfid.text = "RFID ID : $rfid"
        binding.etName.setText(name)
        binding.etHp.setText(phone)
        binding.tvExpired.text = "Expired : $expired"
    }


}