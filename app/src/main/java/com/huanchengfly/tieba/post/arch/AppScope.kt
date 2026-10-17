package com.huanchengfly.tieba.post.arch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/**
 * 进程级协程作用域，生命周期与应用进程一致。
 *
 * 用于必须跑完、不能随页面销毁而取消的后台写入（DataStore 写入、Cookie 落盘等），
 * 替代 GlobalScope / 每次新建的 MainScope()。SupervisorJob 保证单个任务失败不影响其他任务。
 */
val AppScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
