/*
 *  Copyright (c) 2019 ETH Zürich, IT Services
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.webservice.servicelayer.sebconfig;

import java.util.*;

import ch.ethz.seb.sebserver.SEBServerInit;
import ch.ethz.seb.sebserver.SEBServerInitEvent;
import ch.ethz.seb.sebserver.gbl.model.sebconfig.SEBSettingsView;
import ch.ethz.seb.sebserver.gbl.util.Result;
import ch.ethz.seb.sebserver.gbl.util.Utils;
import org.springframework.context.event.EventListener;

public interface SEBSettingsService {

    /** Mapping of all SEB Settings/ConfigurationAttributes for different SEB Settings view.
     *  These are mostly specialised SEB Settings views with fewer Settings than the original view.
     */
    EnumMap<SEBSettingsView.ViewType, Set<Long>> VIEW_ATTRIBUTE_MAPPINGS =
            new EnumMap<>(SEBSettingsView.ViewType.class);

    /** Password attributes id list */
    Set<Long> PASSWORD_TYPE_ATTRIBUTES = new HashSet<>(Arrays.asList(1L, 4L));

    @EventListener(SEBServerInitEvent.class)
    default void init() {

        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Initialize SEB Settings Service...");
        SEBServerInit.INIT_LOGGER.info("------>");

        /* Ids of all SEB Setting/Configuration Attributes for the General view */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.GENERAL,
                Utils.immutableSetOf( Arrays.asList(1L, 2L, 4L, 802L, 920L))
        );

        /* Ids of all SEB Setting/Configuration Attributes for the User Interface view */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.USER_INTERFACE,
                Utils.immutableSetOf( Arrays.asList(8L,
                        10L, 11L, 12L, 13L, 14L, 15L, 16L, 18L,
                        19L, 20L, 21L, 22L, 24L, 25L, 26L, 27L,
                        28L, 29L, 804L, 812L, 951L, 952L, 953L,
                        950L, 974L, 975L, 1595L, 1660L, 1661L,
                        1662L, 1663L, 1664L, 1665L, 17L, 902L, 926L, 927L))
        );

        /* Ids of all SEB Setting/Configuration Attributes for the BROWSER view */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.BROWSER,
                Utils.immutableSetOf( Arrays.asList(8L,
                        31L, 32L, 33L, 34L, 35L, 42L, 43L,
                        44L,45L,46L,47L,48L,50L,51L,52L,
                        53L,54L,55L,56L,57L,58L,960L,961L,
                        919L,928L,904L,1564L,1590L,1568L,1558L,
                        1559L,1560L,1561L,1562L,905L,1569L,
                        1570L,1563L,1602L,1603L, 37L, 39L))
        );

        /* Ids of all SEB Setting/Configuration Attributes for the DOWN_UPLOAD view */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.DOWN_UPLOAD,
                Utils.immutableSetOf( Arrays.asList(8L,
                        59L,60L,61L,972L,63L,64L,65L,66L,
                        1580L,1581L,1582L,1651L,1652L,
                        1653L,1654L,1655L, 62L))
        );

        /* Ids of all SEB Setting/Configuration Attributes for the EXAM view */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.EXAM,
                Utils.immutableSetOf( Arrays.asList(8L,
                        67L,68L,69L,70L,71L,72L,900L,
                        901L,942L,940L,941L,973L))
        );

        /* Ids of all SEB Setting/Configuration Attributes for the Application view */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.APPLICATION,
                Utils.immutableSetOf( Arrays.asList(
                        73L, 74L, 75L, 76L, 77L, 78L, 79L, 81L, 82L, 85L,
                        86L, 87L, 88L, 89L, 90L, 91L, 93L, 94L, 95L,
                        96L, 97L, 98L, 99L, 100L, 1200L, 1577L, 1630L,
                        1631L, 1632L, 1633L, 1634L))
        );

        /* Ids id all SEB Settings/ConfigurationAttributes for the Network vew */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.NETWORK,
                Utils.immutableSetOf( Arrays.asList(
                        200L, 201L, 202L, 203L, 204L, 205L, 206L, 210L, 220L,
                        221L, 222L, 223L, 231L, 233L, 234L, 235L, 236L, 237L,
                        238L, 239L, 240L, 241L, 242L, 243L, 244L, 245L, 246L,
                        247L, 248L, 249L, 250L, 251L, 252L, 253L, 254L, 255L,
                        256L, 257L, 258L, 259L, 260L, 261L, 262L, 263L, 264L,
                        265L, 908L, 929L))
        );

        /* Ids id all SEB Settings/ConfigurationAttributes for the SECURITY vew */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.SECURITY,
                Utils.immutableSetOf( Arrays.asList(
                        300L,301L,302L,303L,947L,305L,306L,307L,308L,
                        309L,310L,311L,312L,313L,314L,315L,316L,317L,
                        318L,319L,320L,321L,322L,501L,971L,1578L,1551L,
                        1567L,1550L,909L,1552L,948L,1557L,943L,945L,
                        1201L,1600L,1620L,1601L,1621L, 921L, 922L, 923L, 924L, 925L))
        );

        /* Ids id all SEB Settings/ConfigurationAttributes for the REGISTRY vew */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.REGISTRY,
                Utils.immutableSetOf( Arrays.asList(
                        400L,401L,402L,403L,404L,405L,406L,
                        407L,408L,970L,1591L))
        );

        /* Ids id all SEB Settings/ConfigurationAttributes for the HOOKED_KEYS vew */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.HOOKED_KEYS,
                Utils.immutableSetOf( Arrays.asList(
                        500L,502L,503L,504L,505L,506L,507L,
                        508L,509L,510L,511L,512L,513L,514L,
                        515L,516L,517L,518L,519L,520L))
        );

        /* Ids id all SEB Settings/ConfigurationAttributes for the PROCTORING vew */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.PROCTORING,
                Utils.immutableSetOf( Arrays.asList(
                        1129L, 1300L, 1301L, 1302L, 1303L, 1305L, 1306L,
                        1320L, 1321L, 1322L, 1323L, 1326L))
        );

    }

    /** Get static list of SEB Setting attribute ids per View */
    default Set<Long> getAttributeIdsForView(final SEBSettingsView.ViewType viewType) {
        return VIEW_ATTRIBUTE_MAPPINGS.get(viewType);
    }

    Result<SEBSettingsView> getSEBSettingsOfTemplate(Long templateId, SEBSettingsView.ViewType viewType);
    Result<SEBSettingsView> getSEBSettingsOfExam(Long examId, SEBSettingsView.ViewType viewType);

    Result<List<SEBSettingsView.TableRowValues>> getTableValuesOfTemplate(Long templateId,  String attributeName);
    Result<List<SEBSettingsView.TableRowValues>> getTableValuesOfExam(Long examId,  String attributeName);
    
    Result<SEBSettingsView.Value> saveSingleValueForTemplate(Long templateId, Long valueId, String value);
    Result<SEBSettingsView.Value> saveSingleValueForExam(Long examId, Long valueId, String value);
    
    Result<SEBSettingsView.TableRowValues> saveTableRowValuesForTemplate(Long templateId, SEBSettingsView.TableRowValues values);
    Result<SEBSettingsView.TableRowValues> saveTableRowValuesForExam(Long examId, SEBSettingsView.TableRowValues values);
    
    Result<SEBSettingsView.TableRowValues> addNewTableRowForTemplate(Long templateId, String attributeName);
    Result<SEBSettingsView.TableRowValues> addNewTableRowForExam(Long examId, String attributeName);

    Result<List<SEBSettingsView.TableRowValues>> deleteTableRowForTemplate(Long templateId, String attributeName, int index);
    Result<List<SEBSettingsView.TableRowValues>> deleteTableRowForExam(Long examId, String attributeName, int index);

    Result<Integer> getActiveSEBClientsForExam(Long examId);
    
    //Result<Long> applySettingsForTemplate(Long templateId);
    Result<Long> applySettingsForExam(Long examId);

    //Result<Long> undoSettingsForTemplate(Long templateId);
    Result<Long> undoSettingsForExam(Long examId);

}
