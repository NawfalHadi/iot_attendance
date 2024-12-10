package space.thatnawfal.iotattendance

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import space.thatnawfal.iotattendance.adapter.MemberAdapter
import space.thatnawfal.iotattendance.data.User
import space.thatnawfal.iotattendance.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null

    private lateinit var memberAdapter: MemberAdapter
    val memberList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(1000)
        installSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        memberAdapter = MemberAdapter(memberList, object : MemberAdapter.OnItemClickListener{
            override fun onItemClick(pos: Int) {
                val member = memberList.get(pos)

                val intent = Intent(this@MainActivity, UpdateActivity::class.java)
                intent.putExtra("rfid", member.rfid)
                intent.putExtra("name", member.name)
                intent.putExtra("phone", member.phone_number)
                intent.putExtra("expired", member.expired_at)

                startActivity(intent)

                Log.d("RECYCLER VIEW", "onItemClick: ${member}")
            }

        })
        binding.rvMember.adapter = memberAdapter
        binding.rvMember.setHasFixedSize(true)
        binding.rvMember.layoutManager = LinearLayoutManager(this)

        binding.fabRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val database = Firebase.database
        val myRef = database.getReference("")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                memberList.clear()

                val value = snapshot.getValue<HashMap<String, Any>>()
                Log.d("FIREBASE KEY", "Value is: {${value?.keys}}")
                Log.d("FIREBASE VALUE", "Value is: {${value?.values}}")

                var counter = 0

                for (data in value!!){
                    val values = data.value
                    val key = data.key

                    val user = values as Map<*, *>
                    val temp = User(
                        rfid = key,
                        active = user["active"] as Long,
                        name = user["name"] as String,
                        phone_number = user["phone_number"] as String,
                        created_at = user["created_at"] as String,
                        expired_at = user["expired_at"] as String,
                        updated_at = user["updated_at"] as String
                  )

                    memberList.add(temp)
                    Log.d("FIREBASE VALUE", "Here : ${key}")
                    counter += 1

                }

                memberAdapter.notifyDataSetChanged()

            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ERROR FIREBASE", "Failed to read value.", error.toException())
            }
        })

    }

    override fun onResume() {
        super.onResume()

        // Create a PendingIntent to handle NFC tag detection
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        // Intent filters for detecting NFC tags
        val intentFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        val filters = arrayOf(intentFilter)

        // Start listening for NFC tags
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        intent.let { super.onNewIntent(it) }

        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

            if (tag != null) {
                val tagId = tag.id.joinToString("") { "%02x".format(it) }
                Log.d("NFC-", "onNewIntent: $tagId")
            }
        }
    }

}