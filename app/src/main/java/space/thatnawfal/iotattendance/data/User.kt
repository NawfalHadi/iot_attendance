package space.thatnawfal.iotattendance.data

data class User(
    val active: Long,
    val name: String?,
    val phone_number: String?,
    val created_at: String?,
    val updated_at: String?,
    val expired_at: String?
)
