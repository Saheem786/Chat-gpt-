package com.example.device

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String
)

sealed class DeviceActionResult {
    data class Success(val action: String, val message: String) : DeviceActionResult()
    data class Failure(val action: String, val error: String) : DeviceActionResult()
    data class ContactFound(val contact: ContactItem, val message: String) : DeviceActionResult()
    data class MultipleContactsFound(val contacts: List<ContactItem>, val query: String) : DeviceActionResult()
    data class ContactNotFound(val query: String) : DeviceActionResult()
    data class PermissionRequired(val permission: String, val reason: String) : DeviceActionResult()
}
