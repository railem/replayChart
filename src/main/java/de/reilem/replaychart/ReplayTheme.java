package de.reilem.replaychart;

import org.knowm.xchart.style.colors.ChartColor;
import org.knowm.xchart.style.theme.XChartTheme;

import java.awt.*;

public class ReplayTheme extends XChartTheme
{
    public Color getChartBackgroundColor() {
        return ChartColor.getAWTColor(ChartColor.LIGHT_GREY);
    }
    public Color getPlotGridLinesColor() {
        return ChartColor.getAWTColor(ChartColor.WHITE);
    }
}
