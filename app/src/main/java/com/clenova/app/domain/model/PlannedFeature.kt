package com.clenova.app.domain.model

/**
 * Clean descriptor for upcoming capability modules without simulated engines.
 */
data class PlannedCapabilityDescriptor(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
)
