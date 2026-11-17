package com.example.game.graphics

import android.opengl.GLES20
import android.util.Log

/**
 * OpenGL ES 2.0 PBR World Shader + Atmospheric Sky Dome Shader.
 * Features Albedo + Normal Map perturbation, Roughness/Metallic BRDF,
 * Schlick Fresnel + Hemispheric Sky Reflection Probe IBL, AO grounding, and atmospheric fog.
 */
class PbrShaderProgram {
    var worldProgram = 0
        private set
    var skyProgram = 0
        private set

    // World attribute & uniform handles
    var aPos = 0; private set
    var aNorm = 0; private set
    var aUv = 0; private set

    var uMvp = 0; private set
    var uModel = 0; private set
    var uDiffuseTex = 0; private set
    var uNormalTex = 0; private set
    var uTintColor = 0; private set
    var uRoughness = 0; private set
    var uMetallic = 0; private set
    var uEmissive = 0; private set
    var uUvScale = 0; private set
    var uUvOffset = 0; private set
    var uUseTex = 0; private set
    var uCameraPos = 0; private set
    var uLightDir = 0; private set
    var uSunColor = 0; private set
    var uSkyZenith = 0; private set
    var uSkyHorizon = 0; private set
    var uGroundBounce = 0; private set
    var uFogColor = 0; private set
    var uFogMaxDist = 0; private set

    // Sky dome handles
    var skyAPos = 0; private set
    var skyUMvp = 0; private set
    var skyULightDir = 0; private set
    var skyUZenith = 0; private set
    var skyUHorizon = 0; private set
    var skyUSunColor = 0; private set
    var skyUTime = 0; private set
    var skyUCloudDensity = 0; private set

    fun init() {
        worldProgram = createProgram(WORLD_VS, WORLD_FS)
        aPos = GLES20.glGetAttribLocation(worldProgram, "aPosition")
        aNorm = GLES20.glGetAttribLocation(worldProgram, "aNormal")
        aUv = GLES20.glGetAttribLocation(worldProgram, "aTexCoord")

        uMvp = GLES20.glGetUniformLocation(worldProgram, "uMVPMatrix")
        uModel = GLES20.glGetUniformLocation(worldProgram, "uModelMatrix")
        uDiffuseTex = GLES20.glGetUniformLocation(worldProgram, "uDiffuseTex")
        uNormalTex = GLES20.glGetUniformLocation(worldProgram, "uNormalTex")
        uTintColor = GLES20.glGetUniformLocation(worldProgram, "uTintColor")
        uRoughness = GLES20.glGetUniformLocation(worldProgram, "uRoughness")
        uMetallic = GLES20.glGetUniformLocation(worldProgram, "uMetallic")
        uEmissive = GLES20.glGetUniformLocation(worldProgram, "uEmissive")
        uUvScale = GLES20.glGetUniformLocation(worldProgram, "uUvScale")
        uUvOffset = GLES20.glGetUniformLocation(worldProgram, "uUvOffset")
        uUseTex = GLES20.glGetUniformLocation(worldProgram, "uUseTex")
        uCameraPos = GLES20.glGetUniformLocation(worldProgram, "uCameraPos")
        uLightDir = GLES20.glGetUniformLocation(worldProgram, "uLightDir")
        uSunColor = GLES20.glGetUniformLocation(worldProgram, "uSunColor")
        uSkyZenith = GLES20.glGetUniformLocation(worldProgram, "uSkyZenith")
        uSkyHorizon = GLES20.glGetUniformLocation(worldProgram, "uSkyHorizon")
        uGroundBounce = GLES20.glGetUniformLocation(worldProgram, "uGroundBounce")
        uFogColor = GLES20.glGetUniformLocation(worldProgram, "uFogColor")
        uFogMaxDist = GLES20.glGetUniformLocation(worldProgram, "uFogMaxDist")

        skyProgram = createProgram(SKY_VS, SKY_FS)
        skyAPos = GLES20.glGetAttribLocation(skyProgram, "aPosition")
        skyUMvp = GLES20.glGetUniformLocation(skyProgram, "uMVPMatrix")
        skyULightDir = GLES20.glGetUniformLocation(skyProgram, "uLightDir")
        skyUZenith = GLES20.glGetUniformLocation(skyProgram, "uZenithColor")
        skyUHorizon = GLES20.glGetUniformLocation(skyProgram, "uHorizonColor")
        skyUSunColor = GLES20.glGetUniformLocation(skyProgram, "uSunColor")
        skyUTime = GLES20.glGetUniformLocation(skyProgram, "uTime")
        skyUCloudDensity = GLES20.glGetUniformLocation(skyProgram, "uCloudDensity")
    }

    private fun createProgram(vsSource: String, fsSource: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSource)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSource)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            Log.e("PbrShaderProgram", "Link failed: ${GLES20.glGetProgramInfoLog(prog)}")
        }
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e("PbrShaderProgram", "Shader compile failed: ${GLES20.glGetShaderInfoLog(s)}")
        }
        return s
    }

    companion object {
        private const val WORLD_VS = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uModelMatrix;
            uniform vec2 uUvScale;
            uniform vec2 uUvOffset;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec2 aTexCoord;
            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            varying vec2 vTexCoord;
            varying float vViewDist;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vWorldPos = worldPos.xyz;
                vWorldNormal = normalize(mat3(uModelMatrix) * aNormal);
                vTexCoord = aTexCoord * uUvScale + uUvOffset;
                vec4 clipPos = uMVPMatrix * vec4(aPosition, 1.0);
                vViewDist = clipPos.w;
                gl_Position = clipPos;
            }
        """

        private const val WORLD_FS = """
            precision mediump float;
            uniform sampler2D uDiffuseTex;
            uniform sampler2D uNormalTex;
            uniform vec4 uTintColor;
            uniform float uRoughness;
            uniform float uMetallic;
            uniform float uEmissive;
            uniform float uUseTex;
            uniform vec3 uCameraPos;
            uniform vec3 uLightDir;
            uniform vec3 uSunColor;
            uniform vec3 uSkyZenith;
            uniform vec3 uSkyHorizon;
            uniform vec3 uGroundBounce;
            uniform vec3 uFogColor;
            uniform float uFogMaxDist;

            varying vec3 vWorldPos;
            varying vec3 vWorldNormal;
            varying vec2 vTexCoord;
            varying float vViewDist;

            void main() {
                vec3 texRgb = texture2D(uDiffuseTex, vTexCoord).rgb;
                vec3 albedo = uTintColor.rgb * mix(vec3(1.0), texRgb, uUseTex);

                // Analytic TBN perturbation without requiring GL_OES_standard_derivatives
                vec3 baseN = normalize(vWorldNormal);
                vec3 upRef = abs(baseN.y) < 0.94 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
                vec3 tangent = normalize(cross(upRef, baseN));
                vec3 bitangent = cross(baseN, tangent);
                vec3 mapN = texture2D(uNormalTex, vTexCoord).xyz * 2.0 - 1.0;
                float bumpWeight = uUseTex * (1.0 - uEmissive * 0.85);
                vec3 N = normalize(mix(baseN, tangent * mapN.x + bitangent * mapN.y + baseN * mapN.z, bumpWeight * 0.72));

                vec3 V = normalize(uCameraPos - vWorldPos);
                vec3 L = normalize(uLightDir);
                vec3 H = normalize(L + V);

                // Ground contact AO + wrap subsurface diffuse shading
                float contactAo = clamp(0.76 + vWorldPos.y * 0.06, 0.72, 1.0);
                float rawNDotL = dot(N, L);
                float nDotL = max(rawNDotL, 0.0);
                float wrapDiffuse = clamp((rawNDotL + 0.22) / 1.22, 0.0, 1.0);
                float nDotV = max(dot(N, V), 0.001);
                float nDotH = max(dot(N, H), 0.0);

                // Roughness-driven Blinn-GGX specular lobe + Schlick Fresnel
                float rough = clamp(uRoughness, 0.06, 0.98);
                float shininess = mix(160.0, 5.0, rough * rough);
                float specNorm = (shininess + 8.0) / 25.13;
                float specPower = pow(nDotH, shininess) * specNorm * 0.16;
                vec3 f0 = mix(vec3(0.04), albedo, uMetallic);
                float fresnel = pow(1.0 - nDotV, 5.0);
                vec3 F = f0 + (vec3(1.0) - f0) * fresnel;

                // Subtle sky rim highlight for crisp realistic 3D character separation
                float rimFactor = pow(1.0 - nDotV, 3.2) * (1.0 - rough * 0.55) * 0.22;
                vec3 rimLight = mix(uSkyHorizon, uSunColor, 0.45) * rimFactor;

                // Hemispheric Global Illumination + Reflection Probe
                vec3 R = reflect(-V, N);
                float reflSkyMix = clamp(R.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 iblSky = mix(uGroundBounce, mix(uSkyHorizon, uSkyZenith, clamp(R.y, 0.0, 1.0)), reflSkyMix);
                float sunGlint = pow(max(dot(R, L), 0.0), mix(52.0, 6.0, rough)) * (1.0 - rough * 0.82);
                vec3 envSpec = (iblSky + uSunColor * sunGlint * 1.35) * F * mix(0.95, 0.22, rough);

                float hemiMix = clamp(N.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 ambientGi = mix(uGroundBounce, mix(uSkyHorizon, uSkyZenith, 0.38), hemiMix) * 0.58;
                vec3 diffuseTerm = albedo * (1.0 - uMetallic * 0.65) * (ambientGi * contactAo + uSunColor * mix(nDotL, wrapDiffuse, 0.35) * 0.94);
                vec3 directSpec = uSunColor * F * specPower * nDotL;

                vec3 litColor = diffuseTerm + directSpec + envSpec + rimLight;
                vec3 emissiveColor = mix(litColor, albedo * (1.0 + uEmissive * 0.40), uEmissive);

                // Distance Atmospheric Haze + Aerial Perspective
                float fogFactor = clamp((vViewDist - 42.0) / max(uFogMaxDist - 42.0, 26.0), 0.0, 0.88);
                vec3 finalRgb = mix(emissiveColor, uFogColor, fogFactor * (1.0 - uEmissive * 0.6));

                gl_FragColor = vec4(clamp(finalRgb, 0.0, 1.0), uTintColor.a);
            }
        """

        private const val SKY_VS = """
            uniform mat4 uMVPMatrix;
            attribute vec3 aPosition;
            varying vec3 vDir;
            void main() {
                vDir = normalize(aPosition);
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
            }
        """

        private const val SKY_FS = """
            precision mediump float;
            uniform vec3 uLightDir;
            uniform vec3 uZenithColor;
            uniform vec3 uHorizonColor;
            uniform vec3 uSunColor;
            uniform float uTime;
            uniform float uCloudDensity;
            varying vec3 vDir;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }
            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                float a = hash(i);
                float b = hash(i + vec2(1.0, 0.0));
                float c = hash(i + vec2(0.0, 1.0));
                float d = hash(i + vec2(1.0, 1.0));
                return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            void main() {
                vec3 dir = normalize(vDir);
                float h = clamp(dir.y, 0.0, 1.0);
                vec3 sky = mix(uHorizonColor, uZenithColor, pow(h, 0.56));

                // Solar / Lunar disc + atmospheric scatter halo
                vec3 L = normalize(uLightDir);
                float sunDot = max(dot(dir, L), 0.0);
                float halo = pow(sunDot, 12.0) * 0.42 + pow(sunDot, 220.0) * 1.45;
                sky += uSunColor * halo;

                // Night Ops Starfield when zenith luminance is dark
                float zenithLum = dot(uZenithColor, vec3(0.299, 0.587, 0.114));
                if (zenithLum < 0.14 && dir.y > 0.06) {
                    vec2 starCell = floor(dir.xz / (dir.y + 0.35) * 85.0);
                    float starRnd = hash(starCell);
                    if (starRnd > 0.986) {
                        float twinkle = 0.65 + 0.35 * sin(uTime * 3.2 + starRnd * 90.0);
                        float nightMask = smoothstep(0.14, 0.04, zenithLum) * smoothstep(0.06, 0.25, dir.y);
                        sky += vec3(0.85, 0.92, 1.0) * twinkle * nightMask * 0.95;
                    }
                }

                // Multi-octave projected volumetric clouds above horizon
                if (dir.y > 0.03) {
                    vec2 cloudUv = dir.xz / (dir.y + 0.18) * 1.55 + vec2(uTime * 0.022, uTime * 0.011);
                    float c = noise(cloudUv * 1.4) * 0.55 + noise(cloudUv * 3.1 + 1.7) * 0.30 + noise(cloudUv * 6.2) * 0.15;
                    float cloudMask = smoothstep(0.50, 0.82, c) * smoothstep(0.03, 0.26, dir.y) * uCloudDensity;
                    vec3 baseCloud = zenithLum < 0.14 ? vec3(0.16, 0.20, 0.30) : vec3(0.95, 0.97, 1.0);
                    vec3 cloudCol = mix(baseCloud, uSunColor * (zenithLum < 0.14 ? 0.55 : 1.08), pow(sunDot, 4.0) * 0.48);
                    sky = mix(sky, cloudCol, cloudMask);
                }

                gl_FragColor = vec4(clamp(sky, 0.0, 1.0), 1.0);
            }
        """
    }
}
