package mchorse.bbs_mod.cubic.animation.legacy.validation;

import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LegacyAnimationValidator
{
    private static final Set<String> AXES = Set.of("x", "y", "z");

    public LegacyAnimationsConfig sanitize(LegacyAnimationsConfig input)
    {
        LegacyAnimationsConfig config = new LegacyAnimationsConfig();

        if (input == null)
        {
            return config;
        }

        config.copy(input);
        config.limbs.entrySet().removeIf((entry) -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null || entry.getValue().isEmpty());

        for (Map.Entry<String, LegacyLimbAnimationConfig> entry : config.limbs.entrySet())
        {
            LegacyLimbAnimationConfig limb = entry.getValue();
            limb.wheelAxis = this.normalizeAxis(limb.wheelAxis);
            limb.wheelSpeed = this.normalizeWheelSpeed(limb.wheelSpeed);
        }

        return config;
    }

    public List<String> validate(LegacyAnimationsConfig config)
    {
        List<String> errors = new ArrayList<>();

        if (config == null)
        {
            errors.add("Legacy animations config is null");

            return errors;
        }

        for (Map.Entry<String, LegacyLimbAnimationConfig> entry : config.limbs.entrySet())
        {
            String limbId = entry.getKey();
            LegacyLimbAnimationConfig limb = entry.getValue();

            if (limbId == null || limbId.isBlank())
            {
                errors.add("Limb id is blank");
                continue;
            }

            if (limb == null)
            {
                errors.add("Limb config is null for " + limbId);
                continue;
            }

            if (!AXES.contains(this.rawAxis(limb.wheelAxis)))
            {
                errors.add("Wheel axis is invalid for " + limbId + ": " + limb.wheelAxis);
            }

            if (Float.isNaN(limb.wheelSpeed) || Float.isInfinite(limb.wheelSpeed) || limb.wheelSpeed < 0F || limb.wheelSpeed > 100F)
            {
                errors.add("Wheel speed is invalid for " + limbId + ": " + limb.wheelSpeed);
            }
        }

        return errors;
    }

    public String toJavascript(LegacyAnimationsConfig config)
    {
        LegacyAnimationsConfig sanitized = this.sanitize(config);
        StringBuilder builder = new StringBuilder();

        builder.append("var legacyAnimations = {\n");
        builder.append("  format: \"bbs.legacy.animations.v1\",\n");
        builder.append("  minecraft: \"1.8+\",\n");
        builder.append("  enabled: ").append(sanitized.enabled).append(",\n");
        builder.append("  limbs: {\n");

        int index = 0;
        int total = sanitized.limbs.size();

        for (Map.Entry<String, LegacyLimbAnimationConfig> entry : sanitized.limbs.entrySet())
        {
            LegacyLimbAnimationConfig limb = entry.getValue();

            builder.append("    \"").append(this.escape(entry.getKey())).append("\": { ");
            builder.append("swinging: ").append(limb.swinging).append(", ");
            builder.append("swiping: ").append(limb.swiping).append(", ");
            builder.append("lookX: ").append(limb.lookX).append(", ");
            builder.append("lookY: ").append(limb.lookY).append(", ");
            builder.append("idle: ").append(limb.idle).append(", ");
            builder.append("invert: ").append(limb.invert).append(", ");
            builder.append("wheel: ").append(limb.wheel).append(", ");
            builder.append("wheelAxis: \"").append(this.escape(limb.wheelAxis)).append("\", ");
            builder.append("wheelSpeed: ").append(this.floatToJs(limb.wheelSpeed)).append(", ");
            builder.append("wheelReverse: ").append(limb.wheelReverse).append(" }");

            index += 1;

            if (index < total)
            {
                builder.append(",");
            }

            builder.append("\n");
        }

        builder.append("  }\n");
        builder.append("};\n");

        return builder.toString();
    }

    public boolean isValidJavascript(String code)
    {
        if (code == null || code.isBlank())
        {
            return false;
        }

        int braces = 0;
        int brackets = 0;
        int parenthesis = 0;
        boolean inString = false;
        char quote = 0;
        boolean escaped = false;

        for (int i = 0; i < code.length(); i++)
        {
            char c = code.charAt(i);

            if (inString)
            {
                if (escaped)
                {
                    escaped = false;
                    continue;
                }

                if (c == '\\')
                {
                    escaped = true;
                    continue;
                }

                if (c == quote)
                {
                    inString = false;
                }

                continue;
            }

            if (c == '"' || c == '\'')
            {
                inString = true;
                quote = c;
                continue;
            }

            if (c == '{') braces += 1;
            else if (c == '}') braces -= 1;
            else if (c == '[') brackets += 1;
            else if (c == ']') brackets -= 1;
            else if (c == '(') parenthesis += 1;
            else if (c == ')') parenthesis -= 1;

            if (braces < 0 || brackets < 0 || parenthesis < 0)
            {
                return false;
            }
        }

        return !inString && braces == 0 && brackets == 0 && parenthesis == 0;
    }

    private float normalizeWheelSpeed(float wheelSpeed)
    {
        if (Float.isNaN(wheelSpeed) || Float.isInfinite(wheelSpeed))
        {
            return 1F;
        }

        if (wheelSpeed < 0F)
        {
            return 0F;
        }

        return Math.min(100F, wheelSpeed);
    }

    private String normalizeAxis(String axis)
    {
        if (axis == null)
        {
            return "x";
        }

        String normalized = axis.trim().toLowerCase();

        if (!AXES.contains(normalized))
        {
            return "x";
        }

        return normalized;
    }

    private String rawAxis(String axis)
    {
        if (axis == null)
        {
            return "";
        }

        return axis.trim().toLowerCase();
    }

    private String floatToJs(float value)
    {
        if (Float.isNaN(value) || Float.isInfinite(value))
        {
            return "1.0";
        }

        if (value == (long) value)
        {
            return String.format("%d.0", (long) value);
        }

        return Float.toString(value);
    }

    private String escape(String value)
    {
        if (value == null)
        {
            return "";
        }

        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
