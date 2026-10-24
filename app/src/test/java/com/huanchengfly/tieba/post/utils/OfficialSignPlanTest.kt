package com.huanchengfly.tieba.post.utils

import com.google.gson.Gson
import com.huanchengfly.tieba.post.api.models.GetForumListBean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialSignPlanTest {
    private fun forum(id: String, level: Int, signed: Boolean) =
        """{"forum_id":"$id","forum_name":"f$id","user_level":"$level","is_sign_in":"${if (signed) 1 else 0}"}"""

    private fun bean(canUse: String = "1", level: String? = "7", step: String? = "2", vararg forums: String) =
        Gson().fromJson(
            buildString {
                append("""{"can_use":"$canUse",""")
                if (level != null) append(""""level":"$level",""")
                if (step != null) append(""""msign_step_num":"$step",""")
                append(""""forum_info":[${forums.joinToString(",")}]}""")
            },
            GetForumListBean::class.java
        )

    @Test
    fun picksUnsignedForumsAtOrAboveLevelInBatches() {
        val plan = planOfficialSign(
            bean(
                forums = arrayOf(
                    forum("1", 7, false),
                    forum("2", 6, false),
                    forum("3", 9, true),
                    forum("4", 8, false),
                    forum("5", 10, false),
                )
            )
        )
        assertEquals(setOf("3"), plan.alreadySigned)
        assertEquals(listOf(listOf("1", "4"), listOf("5")), plan.batches)
    }

    @Test
    fun noBatchesWhenOfficialSignUnavailable() {
        val plan = planOfficialSign(bean(canUse = "0", forums = arrayOf(forum("1", 9, false), forum("2", 9, true))))
        assertEquals(setOf("2"), plan.alreadySigned)
        assertTrue(plan.batches.isEmpty())
    }

    @Test
    fun toleratesMissingFields() {
        val empty = planOfficialSign(Gson().fromJson("{}", GetForumListBean::class.java))
        assertTrue(empty.alreadySigned.isEmpty())
        assertTrue(empty.batches.isEmpty())

        val noStep = planOfficialSign(bean(step = null, forums = Array(60) { forum("$it", 9, false) }))
        assertEquals(listOf(50, 10), noStep.batches.map { it.size })
    }
}
