package org.meds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.meds.data.dao.DAOFactory;
import org.meds.data.domain.LocaleString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Locale {

    private static Logger logger = LogManager.getLogger();

    @Autowired
    private DAOFactory daoFactory;

    private Map<Integer, String> strings;

    public void load() {
        List<LocaleString> localeStrings = daoFactory.getWorldDAO().getLocaleStrings();
        this.strings = new HashMap<>(localeStrings.size());
        for (LocaleString string : localeStrings) {
            this.strings.put(string.getId(), string.getString());
        }
        logger.info("Loaded {} locale strings.", localeStrings.size());
    }

    public String getString(int id) {
        String string = this.strings.get(id);
        if (string == null) {
            return "";
        }
        return string;
    }
}
