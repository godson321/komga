package org.gotson.komga.interfaces.api.rest.dto

data class TaskCountDto(
  val count: Int,
  val countByType: Map<String, Int>,
)
