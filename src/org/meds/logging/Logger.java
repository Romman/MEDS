package org.meds.logging;

public class Logger
{
    private org.apache.logging.log4j.Logger logger;

    public Logger(String name)
    {
        this.logger = org.apache.logging.log4j.LogManager.getLogger(name);
    }

    public org.apache.logging.log4j.Logger getInnerLogger()
    {
        return this.logger;
    }

    public void log(String message, Throwable thr)
    {
        switch (this.logger.getName())
        {
            case "Debug":
                this.logger.debug(message, thr);
                break;
            case "Info":
                this.logger.info(message, thr);
                break;
            case "Warn":
                this.logger.warn(message, thr);
                break;
            case "Error":
                this.logger.error(message, thr);
                break;
            case "Fatal":
                this.logger.fatal(message, thr);
                break;
            default:
                break;
        }
    }

    public void log(String message)
    {
        switch (this.logger.getName())
        {
            case "Debug":
                this.logger.debug(message);
                break;
            case "Info":
                this.logger.info(message);
                break;
            case "Warn":
                this.logger.warn(message);
                break;
            case "Error":
                this.logger.error(message);
                break;
            case "Fatal":
                this.logger.fatal(message);
                break;
            default:
                break;
        }
    }

    public void log(String message, Object... args)
    {
        this.log(String.format(message, args));
    }
}
