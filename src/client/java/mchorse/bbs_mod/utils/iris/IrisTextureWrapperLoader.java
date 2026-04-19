package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.resources.FilteredLink;
import mchorse.bbs_mod.utils.resources.MultiLink;

public class IrisTextureWrapperLoader
{
    public IrisTextureWrapperLoader() {}

    public Link createPrefixedCopy(Link link, String suffix)
    {
        /* If given texture is a multi-link, then let's copy it and replace any of the normal
         * textures with appropriate suffixes */
        if (link instanceof MultiLink multiLink)
        {
            MultiLink newMultiLink = (MultiLink) multiLink.copy();

            for (FilteredLink child : newMultiLink.children)
            {
                if (child.path != null)
                {
                    child.path = this.createPrefixedCopy(child.path, suffix);
                }
            }

            return newMultiLink;
        }

        String basePath = StringUtils.removeExtension(link.path);

        /* If users pick an already suffixed texture (e.g. *_s.png), normalize it to
         * the albedo base name first so generated companions become *_n.png and *_s.png. */
        if (basePath.endsWith("_n") || basePath.endsWith("_s"))
        {
            basePath = basePath.substring(0, basePath.length() - 2);
        }

        return new Link(link.source, basePath + suffix);
    }
}
