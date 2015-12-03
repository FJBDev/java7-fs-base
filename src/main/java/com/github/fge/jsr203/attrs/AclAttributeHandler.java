package com.github.fge.jsr203.attrs;

import com.github.fge.jsr203.StandardAttributeNames;

import java.nio.file.attribute.AclFileAttributeView;

public final class AclAttributeHandler
    extends FileAttributeHandler<AclFileAttributeView>
{
    public AclAttributeHandler(final AclFileAttributeView view)
    {
        super(view);
        addReader(StandardAttributeNames.OWNER, view::getOwner);
        addWriter(StandardAttributeNames.OWNER, view::setOwner);

        addReader(StandardAttributeNames.ACL, view::getAcl);
        addWriter(StandardAttributeNames.ACL, view::setAcl);
    }
}