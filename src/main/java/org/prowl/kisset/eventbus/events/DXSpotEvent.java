package org.prowl.kisset.eventbus.events;


import org.prowl.kisset.core.Node;
import org.prowl.kisset.objects.dxcluster.DXSpot;
import org.prowl.kisset.util.Tools;

public class DXSpotEvent extends BaseEvent {

    DXSpot dxSpot;

    public DXSpotEvent(DXSpot dxSpot) {
        this.dxSpot = dxSpot;
    }

    public DXSpot getDxSpot() {
        return dxSpot;
    }

}
