package org.clever.boot.logging.logback;

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.action.NOPAction;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import org.clever.boot.logging.LoggingInitializationContext;
import org.clever.core.env.Environment;

/**
 * Logback {@link JoranConfigurator}的扩展版本，添加了额外的引导规则。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:11 <br/>
 */
class SpringBootJoranConfigurator extends JoranConfigurator {
    private final LoggingInitializationContext initializationContext;

    SpringBootJoranConfigurator(LoggingInitializationContext initializationContext) {
        this.initializationContext = initializationContext;
    }

    @Override
    public void addInstanceRules(RuleStore rs) {
        super.addInstanceRules(rs);
        Environment environment = this.initializationContext.getEnvironment();
        rs.addRule(new ElementSelector("configuration/appProperty"), new SpringPropertyAction(environment));
        rs.addRule(new ElementSelector("*/appProfile"), new SpringProfileAction(environment));
        rs.addRule(new ElementSelector("*/appProfile/*"), new NOPAction());
    }
}
