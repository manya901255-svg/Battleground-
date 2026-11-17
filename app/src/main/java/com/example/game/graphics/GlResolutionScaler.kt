package com.example.game.graphics

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Offscreen FBO resolution scaler executed strictly on GLThread.
 * Eliminates SurfaceHolder.setFixedSize() buffer-size mismatches with BLASTBufferQueue
 * while preserving real fragment-fillrate scaling for Low/Medium/High/Ultra presets.
 */
class GlResolutionScaler {
    private var fboId = 0
    private var colorTexId = 0
    private var depthRbId = 0
    private var fboW = 0
    private var fboH = 0
    private var passActive = false

    private var blitProgram = 0
    private var aPos = 0
    private var aUv = 0
    private var uTex = 0

    private val quadBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUAD_VERTS.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_VERTS)
            position(0)
        }

    fun reset() {
        fboId = 0
        colorTexId = 0
        depthRbId = 0
        fboW = 0
        fboH = 0
        blitProgram = 0
        passActive = false
    }

    fun beginScaledPass(fullW: Int, fullH: Int, resolutionScale: Float) {
        val safeW = fullW.coerceAtLeast(1)
        val safeH = fullH.coerceAtLeast(1)
        val clampedScale = resolutionScale.coerceIn(0.60f, 1.00f)

        if (clampedScale >= 0.99f || safeW < 64 || safeH < 64) {
            passActive = false
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glViewport(0, 0, safeW, safeH)
            return
        }

        val targetW = (safeW * clampedScale).toInt().coerceIn(320, safeW)
        val targetH = (safeH * clampedScale).toInt().coerceIn(180, safeH)
        if (!ensureResources(targetW, targetH)) {
            passActive = false
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glViewport(0, 0, safeW, safeH)
            return
        }

        passActive = true
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, fboW, fboH)
    }

    fun endScaledPassAndPresent(fullW: Int, fullH: Int) {
        if (!passActive) return
        passActive = false

        val safeW = fullW.coerceAtLeast(1)
        val safeH = fullH.coerceAtLeast(1)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, safeW, safeH)

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDepthMask(false)

        GLES20.glUseProgram(blitProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorTexId)
        GLES20.glUniform1i(uTex, 0)

        quadBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 16, quadBuffer)

        quadBuffer.position(2)
        GLES20.glEnableVertexAttribArray(aUv)
        GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 16, quadBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aUv)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
    }

    private fun ensureResources(targetW: Int, targetH: Int): Boolean {
        if (blitProgram == 0) {
            blitProgram = createProgram()
            if (blitProgram == 0) return false
            aPos = GLES20.glGetAttribLocation(blitProgram, "aPosition")
            aUv = GLES20.glGetAttribLocation(blitProgram, "aTexCoord")
            uTex = GLES20.glGetUniformLocation(blitProgram, "uTexture")
        }

        if (fboId != 0 && fboW == targetW && fboH == targetH) {
            return true
        }

        releaseFboOnly()

        val fbos = IntArray(1)
        val texs = IntArray(1)
        val rbs = IntArray(1)
        GLES20.glGenFramebuffers(1, fbos, 0)
        GLES20.glGenTextures(1, texs, 0)
        GLES20.glGenRenderbuffers(1, rbs, 0)

        fboId = fbos[0]
        colorTexId = texs[0]
        depthRbId = rbs[0]
        fboW = targetW
        fboH = targetH

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, colorTexId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            targetW, targetH, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRbId)
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, targetW, targetH)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, colorTexId, 0
        )
        GLES20.glFramebufferRenderbuffer(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
            GLES20.GL_RENDERBUFFER, depthRbId
        )

        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            releaseFboOnly()
            return false
        }
        return true
    }

    private fun releaseFboOnly() {
        if (fboId != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            fboId = 0
        }
        if (colorTexId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(colorTexId), 0)
            colorTexId = 0
        }
        if (depthRbId != 0) {
            GLES20.glDeleteRenderbuffers(1, intArrayOf(depthRbId), 0)
            depthRbId = 0
        }
        fboW = 0
        fboH = 0
    }

    private fun createProgram(): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, BLIT_VS)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, BLIT_FS)
        if (vs == 0 || fs == 0) return 0
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        if (ok[0] == 0) {
            GLES20.glDeleteProgram(p)
            return 0
        }
        return p
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            GLES20.glDeleteShader(s)
            return 0
        }
        return s
    }

    private companion object {
        val QUAD_VERTS = floatArrayOf(
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        )

        const val BLIT_VS = """
            attribute vec2 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                vTexCoord = aTexCoord;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """

        const val BLIT_FS = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vTexCoord;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
