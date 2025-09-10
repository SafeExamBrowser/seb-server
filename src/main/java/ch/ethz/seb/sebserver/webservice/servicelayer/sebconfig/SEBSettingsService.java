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
import org.springframework.context.event.EventListener;

public interface SEBSettingsService {

    /** Mapping of all SEB Settings/ConfigurationAttributes for different SEB Settings view.
     *  These are mostly specialised SEB Settings views with fewer Settings than the original view.
     */
    EnumMap<SEBSettingsView.ViewType, Set<Long>> VIEW_ATTRIBUTE_MAPPINGS =
            new EnumMap<>(SEBSettingsView.ViewType.class);

    @EventListener(SEBServerInitEvent.class)
    default void init() {

        SEBServerInit.INIT_LOGGER.info("------>");
        SEBServerInit.INIT_LOGGER.info("------> Initialize SEB Settings Service...");
        SEBServerInit.INIT_LOGGER.info("------>");
        
        /* Ids of all SEB Setting/Configuration Attributes for the Application view */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.APPLICATION,
                new HashSet<>(Arrays.asList(
                        73L, 74L, 75L, 76L, 77L, 78L, 79L, 81L, 82L, 85L,
                        86L, 87L, 88L, 89L, 90L, 91L, 92L, 93L, 94L, 95L,
                        96L, 97L, 98L, 99L, 100L, 1200L, 1577L, 1630L,
                        1631L, 1632L, 1633L, 1634L))
        );

        /* Ids id all SEB Settings/ConfigurationAttributes for the Network vew */
        VIEW_ATTRIBUTE_MAPPINGS.put(
                SEBSettingsView.ViewType.NETWORK,
                new HashSet<>(Arrays.asList(
                        200L, 201L, 202L, 203L, 204L, 205L, 206L, 210L, 220L,
                        221L, 222L, 223L, 231L, 233L, 234L, 235L, 236L, 237L,
                        238L, 239L, 240L, 241L, 242L, 243L, 244L, 245L, 246L,
                        247L, 248L, 249L, 250L, 251L, 252L, 253L, 254L, 255L,
                        256L, 257L, 258L, 259L, 260L, 261L, 262L, 263L, 264L,
                        265L
                ))
        );
    }
    
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
    
    Result<Long> applySettingsForTemplate(Long templateId);
    Result<Long> applySettingsForExam(Long examId);

    Result<Long> undoSettingsForTemplate(Long templateId);
    Result<Long> undoSettingsForExam(Long examId);

}
