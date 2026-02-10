package org.gotson.komga.interfaces.api.rest.dto

import jakarta.validation.constraints.NotBlank
import org.gotson.komga.domain.service.PathMigrationResult

data class PathMigrationDto(
  val oldPathPrefix: String? = null,
  @get:NotBlank
  val newPathPrefix: String,
)

data class PathMigrationResultDto(
  val libraryUpdated: Int,
  val seriesUpdated: Int,
  val booksUpdated: Int,
  val sidecarsUpdated: Int,
  val mediaReset: Int,
)

fun PathMigrationResult.toDto() =
  PathMigrationResultDto(
    libraryUpdated = libraryUpdated,
    seriesUpdated = seriesUpdated,
    booksUpdated = booksUpdated,
    sidecarsUpdated = sidecarsUpdated,
    mediaReset = mediaReset,
  )
