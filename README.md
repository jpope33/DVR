# DVR
Distance Vector Routing Protocol

So I think we have a decent start here. Currently the protocol consistently produces the fully correct final distance vector 'cost' values, but some incorrect final 'next hop' values. I've ran a few different test scenarios and the same results occur.

-Ran with different starting orders
-Ran with stagger start where multiple nodes have made their first send before other nodes were started

The incorrect 'next hop' values always happen on nodes with 3 connections.
