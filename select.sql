with input as (select to_date('%s', 'DD.MM.YYYY') as x, to_date('%s', 'DD.MM.YYYY') as y from dual),
     qwerty as (select
                       case when x < y then x else y end as x,
                       case when x < y then y else x end as y,
                       to_date('01.01.0001', 'DD.MM.YYYY') as zero,
                       to_date('05.10.1582', 'DD.MM.YYYY') as popa,
                       to_date('28.02.0004', 'DD.MM.YYYY') as viso
                from input),
     kek as (select abs(months_between(x, y)) as diff from qwerty)
select trunc(diff / 12) || ' ' || mod(trunc(abs(x - y) / 30.5), 12) || ' ' || trunc((mod(abs(x - y), 30.5))) from kek, qwerty