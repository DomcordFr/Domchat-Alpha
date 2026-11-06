/*
 * Ported from Convx (com.convx.music.ui.component.backdrop.RuntimeShaderCache)
 * Originally vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Adapted package name com.convx.music.ui.component.backdrop -> me.lucid.domchat.ui.component.backdrop.
 */
package me.lucid.domchat.ui.component.backdrop

import org.intellij.lang.annotations.Language

sealed interface RuntimeShaderCache {

    fun obtainRuntimeShader(key: String, @Language("AGSL") string: String): RuntimeShader
}

/** Thread-safe cache of compiled AGSL RuntimeShaders keyed by shader source. */
internal class RuntimeShaderCacheImpl : RuntimeShaderCache {

    private val runtimeShaders = mutableMapOf<String, RuntimeShader>()

    override fun obtainRuntimeShader(key: String, string: String): RuntimeShader {
        return synchronized(runtimeShaders) {
            runtimeShaders.getOrPut(key) { RuntimeShader(string) }
        }
    }

    fun clear() {
        synchronized(runtimeShaders) {
            runtimeShaders.clear()
        }
    }
}
