package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;

public class ShaderCurves
{
    public static Map<String, ShaderVariable> variableMap = new HashMap<>();

    private static Set<String> prohibitedVariables = new HashSet<>();
    private static Set<String> prohibitedConstIdentifiers = new HashSet<>();

    public static final String BRIGHTNESS = "brightness";
    public static final String SUN_ROTATION = "sun_rotation";
    public static final String SUN_PATH_ROTATION = "sun_path_rotation";
    public static final String WEATHER = "weather";
    public static final String SHADER_SHADOW_OPACITY = "shader_shadow_opacity";

    public static final String UNIFORM_IDENTIFIER = "bbs_";

    static
    {
        /* photon & Hysteria */
        prohibitedVariables.add("WATER_WAVE_ITERATIONS");
        prohibitedConstIdentifiers.add("get_luminance_from_exposure");
        prohibitedConstIdentifiers.add("get_exposure_from_luminance");
    }

    public static void reset()
    {
        variableMap.clear();
    }

    public static void finishLoading()
    {
        ShaderOpacityPatch.ensureShadowOpacityVariable();
        ensureSunPathRotationVariable();
    }

    public static String processSource(String source)
    {
        if (!BBSSettings.shaderCurvesEnabled.get())
        {
            return processSunPathRotation(source);
        }

        Map<String, ShaderVariable> variables = parseVariables(source);

        if (!variables.isEmpty())
        {
            removeIrrelevantVariables(source, variables);

            source = replaceMacroReferences(source, variables);
            source = removeConstFromRelevantVariables(source);
            source = insertUniforms(source, variables);

            for (ShaderVariable value : variables.values())
            {
                variableMap.putIfAbsent(value.name, value);
            }
        }

        return processSunPathRotation(source);
    }

    public static void ensureSunPathRotationVariable()
    {
        ShaderVariable variable = variableMap.get(SUN_PATH_ROTATION);

        if (variable == null)
        {
            variable = new ShaderVariable(SUN_PATH_ROTATION, "0.0", false);
            variableMap.put(SUN_PATH_ROTATION, variable);
        }
    }

    /**
     * Complementary/BSL: yaw sun direction with BBS light yaw.
     * Shadow-map alignment is handled in Iris {@code ShadowMatrices} (cascades need it).
     */
    public static String processSunPathRotation(String source)
    {
        if (source == null || source.isEmpty())
        {
            return source;
        }

        /* Shared sun-direction expression (Complementary GetSunVector body + BSL inlined sunVec). */
        String sunDirExpr = "vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0";
        String sunDirExprYaw = "bbsApplySunPathYaw(vec3(-sin(ang), cos(ang) * sunRotationData)) * 2000.0";
        boolean hasSunDirExpr = source.contains(sunDirExpr) && !source.contains(sunDirExprYaw);
        boolean hasSunVectorFn = source.contains("GetSunVector");
        boolean hasSunPathHelpers = source.contains("bbsApplySunPathYaw");
        boolean needsPatch = hasSunDirExpr || hasSunVectorFn || hasSunPathHelpers;

        if (!needsPatch)
        {
            return source;
        }

        ensureSunPathRotationVariable();

        String uniform = UNIFORM_IDENTIFIER + SUN_PATH_ROTATION;
        /* Uniform MUST come before helpers in one insert — separate inserts put helpers first. */
        String patched = ensureSunPathPreamble(source, uniform);

        if (hasSunVectorFn)
        {
            String overworldReturn =
                "return normalize((gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData) * 2000.0, 1.0)).xyz);";
            String endReturn =
                "return normalize((gbufferModelView * vec4(vec3(0.0, sunRotationData * 2000.0), 1.0)).xyz);";
            String overworldPatched =
                "vec3 bbsSunDir = bbsApplySunPathYaw(vec3(-sin(ang), cos(ang) * sunRotationData));\n"
                    + "            return normalize((gbufferModelView * vec4(bbsSunDir * 2000.0, 1.0)).xyz);";
            String endPatched =
                "return normalize((gbufferModelView * vec4(bbsApplySunPathYaw(vec3(0.0, sunRotationData)) * 2000.0, 1.0)).xyz);";

            patched = patched.replace(overworldReturn, overworldPatched);
            patched = patched.replace(endReturn, endPatched);

            String skyRaw =
                "vec3 rawSunVec2 = (gbufferModelView * vec4(vec3(-sin(ang), cos(ang) * sunRotationData2) * 2000.0, 1.0)).xyz;";
            String skyRawPatched =
                "vec3 rawSunVec2 = (gbufferModelView * vec4(bbsApplySunPathYaw(vec3(-sin(ang), cos(ang) * sunRotationData2)) * 2000.0, 1.0)).xyz;";

            patched = patched.replace(skyRaw, skyRawPatched);
        }

        /* BSL inlines sunVec / nextSunVec (and vxModelView variants) with this shared expression. */
        if (patched.contains(sunDirExpr) && !patched.contains(sunDirExprYaw))
        {
            patched = patched.replace(sunDirExpr, sunDirExprYaw);
        }

        return patched;
    }

    /**
     * Inserts uniform then yaw helper in one block after {@code #version}.
     */
    private static String ensureSunPathPreamble(String source, String uniform)
    {
        boolean hasUniform = source.contains("uniform float " + uniform)
            || source.contains("uniform int " + uniform);
        boolean hasYaw = source.contains("bbsApplySunPathYaw");

        if (hasUniform && hasYaw)
        {
            return source;
        }

        StringBuilder preamble = new StringBuilder();

        if (!hasUniform)
        {
            preamble.append("uniform float ").append(uniform).append(";\n");
        }

        if (!hasYaw)
        {
            preamble.append("#ifndef BBS_SUN_PATH_YAW_HELPER\n");
            preamble.append("#define BBS_SUN_PATH_YAW_HELPER\n");
            preamble.append("vec3 bbsApplySunPathYaw(vec3 dir){\n");
            preamble.append("    float bbsYaw = ").append(uniform).append(" * 0.01745329251994;\n");
            preamble.append("    float bbsC = cos(bbsYaw);\n");
            preamble.append("    float bbsS = sin(bbsYaw);\n");
            preamble.append("    return vec3(dir.x * bbsC - dir.z * bbsS, dir.y, dir.x * bbsS + dir.z * bbsC);\n");
            preamble.append("}\n");
            preamble.append("#endif\n");
        }

        if (preamble.length() == 0)
        {
            return source;
        }

        int version = source.indexOf("#version");

        if (version >= 0)
        {
            int nextNewLine = source.indexOf('\n', version);

            if (nextNewLine >= 0)
            {
                return source.substring(0, nextNewLine + 1) + preamble + source.substring(nextNewLine + 1);
            }
        }

        return preamble + source;
    }

    private static void removeIrrelevantVariables(String source, Map<String, ShaderVariable> variables)
    {
        /* Remove irrelevant variables */
        List<String> filter = BBSRendering.getShadersSliderOptions();

        variables.values().removeIf((v) -> !filter.contains(v.name));

        for (String prohibitedVariable : prohibitedVariables)
        {
            variables.remove(prohibitedVariable);
        }

        int index = 0;

        while ((index = source.indexOf("#", index + 1)) != -1)
        {
            int newLine = source.indexOf('\n', index);

            if (newLine >= 0)
            {
                String substr = source.substring(index, newLine);

                if (substr.startsWith("#if") || substr.startsWith("#elif"))
                {
                    variables.values().removeIf((v) -> substr.contains(v.name));
                }
                else if (substr.startsWith("#define"))
                {
                    final int WHITESPACE = 0, CHARACTERS = 1;
                    int iindex = 7;
                    int state = 0;
                    int switches = 0;

                    while (iindex < newLine - index)
                    {
                        char c = substr.charAt(iindex);

                        if (state == WHITESPACE && Character.isWhitespace(c))
                        {
                            state = CHARACTERS;
                            switches += 1;
                        }
                        else if (Character.isWhitespace(c))
                        {
                            state = WHITESPACE;
                        }

                        if (switches == 2)
                        {
                            break;
                        }

                        iindex += 1;
                    }

                    final String subsubstr = substr.substring(iindex);

                    variables.values().removeIf((v) -> subsubstr.contains(v.name));
                }
            }
        }
    }

    private static Map<String, ShaderVariable> parseVariables(String source)
    {
        Map<String, ShaderVariable> variables = new HashMap<>();
        Pattern definePattern = Pattern.compile("^\\s*(?!//)\\s*#define +([\\w_]+) +([\\d.]+) *// *(\\[|OptionAnnotatedSource)");
        int index = 0;

        while ((index = source.indexOf("#define", index)) != -1)
        {
            int newLine = source.indexOf("\n", index);

            if (newLine == -1)
            {
                newLine = source.length();
            }

            int lastNewLine = source.lastIndexOf('\n', index);
            String define = source.substring(lastNewLine != -1 ? lastNewLine : index, newLine).trim();
            Matcher matcher = definePattern.matcher(define);

            if (matcher.find())
            {
                String name = matcher.group(1);
                String defaultValue = matcher.group(2);
                boolean integer = !defaultValue.contains(".");
                ShaderVariable variable = new ShaderVariable(name, defaultValue, integer);

                variables.putIfAbsent(variable.name, variable);
            }

            index = newLine;
        }

        return variables;
    }

    private static String replaceMacroReferences(String source, Map<String, ShaderVariable> variables)
    {
        StringBuilder out = new StringBuilder(source.length());
        int length = source.length();
        int i = 0;
        boolean macro = false;

        while (i < length)
        {
            char c = source.charAt(i);

            if (c == '#') macro = true;
            if (c == '\n') macro = false;

            if (isIdentifierStart(c) && !macro)
            {
                int start = i;
                int j = i + 1;

                while (j < length && isIdentifierPart(source.charAt(j)))
                {
                    j++;
                }

                String identifier = source.substring(start, j);
                String replacement = variables.containsKey(identifier) ? UNIFORM_IDENTIFIER  + identifier : identifier;

                out.append(replacement);

                i = j;
            }
            else
            {
                out.append(c);

                i++;
            }
        }

        return out.toString();
    }

    private static boolean isIdentifierStart(char c)
    {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentifierPart(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static String removeConstFromRelevantVariables(String source)
    {
        Pair<String, Set<String>> pair = removeConst(source, (s) -> s.contains("bbs_"));
        Set<String> deconst = pair.b;

        source = pair.a;

        for (String constIdentifier : prohibitedConstIdentifiers)
        {
            deconst.add(constIdentifier);
        }

        while (!deconst.isEmpty())
        {
            final Set<String> finalDeconst = deconst;

            pair = removeConst(source, (s) ->
            {
                for (String string : finalDeconst)
                {
                    if (s.contains(string)) return true;
                }

                return false;
            });
            source = pair.a;
            deconst = pair.b;
        }

        return source;
    }

    private static Pair<String, Set<String>> removeConst(String source, Function<String, Boolean> function)
    {
        Set<String> deconst = new HashSet<>();
        StringBuilder builder = new StringBuilder();
        int index = 0;
        int lastIndex = 0;

        while ((index = source.indexOf("const ", index + 1)) != -1)
        {
            int semicolon = source.indexOf(';', index);

            if (semicolon >= 0)
            {
                String substr = source.substring(index, semicolon);

                if (substr.indexOf('{') == -1 && function.apply(substr))
                {
                    builder.append(source, lastIndex, index);
                    builder.append(source, index + 6, semicolon);

                    int equals = substr.indexOf('=');
                    String sub = substr.substring(0, equals).trim();

                    equals = sub.lastIndexOf(' ');
                    sub = sub.substring(equals).trim();

                    deconst.add(sub);
                }
                else
                {
                    builder.append(source, lastIndex, semicolon);
                }
            }

            lastIndex = semicolon;
        }

        builder.append(source, lastIndex, source.length());

        return new Pair<>(builder.toString(), deconst);
    }

    private static String insertUniforms(String source, Map<String, ShaderVariable> variables)
    {
        int version = source.indexOf("#version");
        int nextNewLine = source.indexOf('\n', version);
        StringBuilder sb = new StringBuilder();

        for (ShaderVariable variable : variables.values())
        {
            sb.append(variable.toUniformDeclaration());
            sb.append('\n');
        }

        return source.substring(0, nextNewLine + 1) + sb + source.substring(nextNewLine + 1);
    }

    public static void addUniforms(List<CachedUniform> list)
    {
        BBSRendering.addUniforms(list, variableMap);
    }

    public static class ShaderVariable
    {
        public String name = "";
        public String uniformName = "";
        public boolean integer;
        public float defaultValue;
        public Float value;

        public ShaderVariable(String name, String defaultValue, boolean integer)
        {
            this.name = name;
            this.uniformName = UNIFORM_IDENTIFIER + name;
            this.defaultValue = Float.parseFloat(defaultValue);
            this.integer = integer;
        }

        public String toUniformDeclaration()
        {
            return "uniform " + (this.integer ? "int" : "float") + " " + this.uniformName + ";";
        }

        public float getValue()
        {
            if (this.value == null)
            {
                return this.defaultValue;
            }

            float v = this.value;

            this.value = null;

            return v;
        }
    }
}