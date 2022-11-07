import de.hybris.platform.hmc.model.SavedValuesModel;
import de.hybris.platform.processengine.model.BusinessProcessModel;
import de.hybris.platform.processengine.model.ProcessTaskLogModel;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery
import org.apache.commons.collections.CollectionUtils;
import java.time.ZonedDateTime


de.hybris.platform.tx.Transaction.current().commit()

def now = ZonedDateTime.now()
def fsq = spring.getBean("flexibleSearchService")


// --- TYPES ---

def TYPES = [

        'SavedValues' : [
                'query' : "select {sv.pk} from {SavedValues as sv}  where {sv.creationTime} < ?retentionTime ",
                'parameters' : {
                    ['retentionTime':Date.from(now.minusYears(4).toInstant()) ] // years to be configured like Config.getParameter(key,value)
                },
                'resultClassList': [SavedValuesModel.class],
                'check' : { r ->
                    if (CollectionUtils.isNotEmpty(r.result)) {
                        println("Found ${r.getResult().size()} SavedValues to be deleted")
                    }
                }
        ],
        'ProcessTaskLog' : [
                'query' : "select {ptl.pk} from {ProcessTaskLog as ptl}  where {ptl.creationTime} < ?retentionTime ",
                'parameters' : {
                    ['retentionTime':Date.from(now.minusYears(4).toInstant()) ] // years to be configured like Config.getParameter(key,value)
                },
                'resultClassList': [ProcessTaskLogModel.class],
                'check' : { r ->
                    if (CollectionUtils.isNotEmpty(r.result)) {
                        println("Found ${r.getResult().size()} ProcessTaskLog to be deleted")
                    }
                }
        ]

]

def t = tenantAwareThreadFactory.newThread(new Runnable() {
    @Override
    void run() {
        TYPES.each { type, check ->

            println("--- Checking ${type} ---")

            final FlexibleSearchQuery query = new FlexibleSearchQuery(check.query)
            query.setCount(500000);
            query.setResultClassList(check.resultClassList)

            if (check.parameters) {

                query.addQueryParameters(check.parameters())
            }

            def result = fsq.search(query)
            check.check(result)

            for (item in result.getResult()){

                modelService.remove(item)

            }

            println "------Finished! -----"


        }
    }

})
t.daemon = true
t.start()
println "Started check!"

return null
