Na implementação do exercicio tres é importante destacar os seguintes aspetos :

- Na alteração de threds para corrotinas, foi primeiramente criado na classe `Server` um ***coroutine scope***,
  utilizando como dispatcher o `Default dispatcher`.
- Em seguida, foi realizado, no ***init*** da mesma classe, dois ***launchs***, ou seja, foram criadas duas corrotinas,
  sendo que a segunda é filha da primeira.
- Posteriormente, foi criado um ***supervisor*** scope na função ***controlLoop***, passando a scope para ser
  futuramente utilizada na criação de um ***remoteClient***. Este scope permite que as corrotinas iniciadas dentro de
  si, isto é, serem suas filhas, se uma terminar com algum tipo de exceção não provoca o cancelamento do scope nem das
  outras coroutinas filhas.
- Na classe `RemoteClient`, foi igualmente criado duas corrotinas, onde a segunda também é filha da primeira.

É importante também destacar relativo aos requesitos opcionais o seguinte:

- Para a realização do primeiro requesito, foi criada a função `getSubs`. Esta função recebe como parâmetro o nome do
  tópico a ser publicado e chama a função `getSubscribersFor` que retorna um set com os subscritores associados ao
  tópico, permitindo assim saber a quantidade de subscritores.
- Para a realização do segundo requesito, foi criada a classe `ClientAndTopic`. Esta contém as funções que fazem o
  controlo dos tópicos e dos subscritores. Ou seja, antes esta gestão era realizada pela class server, logo foi
  necessário remover estas funções movendo-as para esta nova classe.