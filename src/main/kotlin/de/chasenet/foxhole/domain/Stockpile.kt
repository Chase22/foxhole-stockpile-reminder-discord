package de.chasenet.foxhole.domain

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

data class Stockpile(
    val code: String,
    val name: String,
    val location: Location,
    val lastReset: Instant
)

data class Location(
    val hex: String,
    val city: String
)

val RESERVATION_EXPIRATION_TIME = 2.days + 4.hours

val HEXES = listOf(
    "The Fingers",
    "Great March",
    "Tempest Island",
    "Marban Hollow",
    "ViperPit",
    "Basin Sionnach",
    "Dead Lands",
    "Heartlands",
    "Endless Shore",
    "Westgate",
    "Oarbreaker",
    "Acrithia",
    "Mooring County",
    "Weathered Expanse",
    "Loch Mor",
    "Morgens Crossing",
    "Stonecradle",
    "Allods Bight",
    "Kalokai",
    "Red River",
    "Origin",
    "Howl County",
    "Shackled Chasm",
    "Speaking Woods",
    "Terminus",
    "Linn Mercy",
    "Clanshead Valley",
    "Godcrofts",
    "Nevish Line",
    "Callums Cape",
    "Fishermans Row",
    "Umbral Wildwood",
    "Reaching Trail",
    "Callahans Passage",
    "Ash Fields",
    "Drowned Vale",
    "Farranac Coast"
)