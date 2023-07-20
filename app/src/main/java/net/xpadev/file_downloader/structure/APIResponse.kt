package net.xpadev.file_downloader.structure

import kotlinx.serialization.Serializable

@Serializable
data class TargetListResponse(
    val markAsComplete: String,
    val data: List<Item>
)

@Serializable
data class Item(
    val link: String,
    val id: Int
)