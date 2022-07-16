package org.clever.boot.logging.logback;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.event.InPlayListener;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.util.OptionHelper;
import org.clever.core.env.Environment;
import org.clever.core.env.Profiles;
import org.clever.util.Assert;
import org.clever.util.StringUtils;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Logback {@link Action}支持 {@code <cleverProfile>} 标签。
 * 仅当特定配置文件处于活动状态时，才允许启用logback配置的部分。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:12 <br/>
 */
class SpringProfileAction extends Action implements InPlayListener {
    private final Environment environment;
    private int depth = 0;
    private boolean acceptsProfile;
    private List<SaxEvent> events;

    SpringProfileAction(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void begin(InterpretationContext ic, String name, Attributes attributes) {
        this.depth++;
        if (this.depth != 1) {
            return;
        }
        ic.pushObject(this);
        this.acceptsProfile = acceptsProfiles(ic, attributes);
        this.events = new ArrayList<>();
        ic.addInPlayListener(this);
    }

    private boolean acceptsProfiles(InterpretationContext ic, Attributes attributes) {
        if (this.environment == null) {
            return false;
        }
        String[] profileNames = StringUtils
                .trimArrayElements(StringUtils.commaDelimitedListToStringArray(attributes.getValue(NAME_ATTRIBUTE)));
        if (profileNames.length == 0) {
            return false;
        }
        for (int i = 0; i < profileNames.length; i++) {
            profileNames[i] = OptionHelper.substVars(profileNames[i], ic, this.context);
        }
        return this.environment.acceptsProfiles(Profiles.of(profileNames));
    }

    @Override
    public void end(InterpretationContext ic, String name) throws ActionException {
        this.depth--;
        if (this.depth != 0) {
            return;
        }
        ic.removeInPlayListener(this);
        verifyAndPop(ic);
        if (this.acceptsProfile) {
            addEventsToPlayer(ic);
        }
    }

    private void verifyAndPop(InterpretationContext ic) {
        Object o = ic.peekObject();
        Assert.state(o != null, "Unexpected null object on stack");
        Assert.isInstanceOf(SpringProfileAction.class, o, "logback stack error");
        Assert.state(o == this, "ProfileAction different than current one on stack");
        ic.popObject();
    }

    private void addEventsToPlayer(InterpretationContext ic) {
        Interpreter interpreter = ic.getJoranInterpreter();
        this.events.remove(0);
        this.events.remove(this.events.size() - 1);
        interpreter.getEventPlayer().addEventsDynamically(this.events, 1);
    }

    @Override
    public void inPlay(SaxEvent event) {
        this.events.add(event);
    }
}

