*******************************
Network Functions and Operators
*******************************

=============
Prerequisites
=============

Apache Tajo provides network functions and operations using GeoIP databases.
To use these functions and operations, the GeoIP database should be precedently installed in local disks of
all the workers.
(Please refer the install instruction in http://dev.maxmind.com/geoip/legacy/downloadable/)

Once the GeoIP database is installed, you should specify the install location in ``conf/tajo-site.xml``
as follows. ::

  <property>
    <name>tajo.function.geoip-database-location</name>
    <value>/path/to/geoip/database/file</value>
  </property>

===================
Supported Functions
===================

.. function:: geoip_country_code (string addr)

  Convert an ipv4 address string to a geoip country code.

  :param addr: ipv4 address string
  :rtype: text
  :example:

  .. code-block:: sql

    select geoip_country_code('163.152.71.31')
    > 'KR'

.. function:: geoip_country_code (inet4 addr)

  Convert an ipv4 address to a geoip country code.

  :param addr: ipv4 address
  :rtype: text
  :example:

  .. code-block:: sql

    select geoip_country_code(163.152.71.31)
    > 'KR'

.. function:: geoip_in_country (string addr, string code)

  If the given country code is same with the country code of the given address, it returns true. Otherwise, returns false.

  :param addr: ipv4 address string
  :param code: country code
  :rtype: boolean
  :example:

  .. code-block:: sql

    select geoip_in_country('163.152.71.31', 'KR')
    > true

.. function:: geoip_in_country (inet4 addr, string code)

  If the given country code is same with the country code of the given address, it returns true. Otherwise, returns false.

  :param addr: ipv4 address
  :param code: country code
  :rtype: boolean
  :example:

  .. code-block:: sql

    select geoip_in_country(163.152.71.31, 'KR')
    > true