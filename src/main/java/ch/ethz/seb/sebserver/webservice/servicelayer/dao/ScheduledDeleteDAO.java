package ch.ethz.seb.sebserver.webservice.servicelayer.dao;



import ch.ethz.seb.sebserver.gbl.model.exam.ScheduledDelete;
import ch.ethz.seb.sebserver.gbl.util.Nullable;
import ch.ethz.seb.sebserver.gbl.util.Result;

public interface ScheduledDeleteDAO extends EntityDAO<ScheduledDelete, ScheduledDelete> {

    //Result<ScheduledDelete> addInfo(Long scheduledDeleteId, Collection<ScheduledDeleteInfo> info);

    Result<Nullable<ScheduledDelete>> getPendingScheduledDelete();

    boolean startProcessing(Long deleteId);
    boolean endProcessing(Long deleteId);

    boolean markAs(Long id, ScheduledDelete.State state);
    boolean startSingleDeletion(Long infoId);
    boolean endSingleDeletion(Long infoId, String errorInfo);


}
